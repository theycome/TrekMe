package com.peterlaurence.trekme.core.billing.data.api

import android.app.Application
import android.util.Log
import arrow.core.raise.Raise
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED
import com.android.billingclient.api.BillingClient.BillingResponseCode.OK
import com.android.billingclient.api.BillingClient.BillingResponseCode.SERVICE_DISCONNECTED
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.peterlaurence.trekme.core.billing.data.model.BillingParams
import com.peterlaurence.trekme.core.billing.data.model.PurchaseIdsContract
import com.peterlaurence.trekme.core.billing.data.model.PurchaseIdsResolver
import com.peterlaurence.trekme.core.billing.data.model.PurchaseType
import com.peterlaurence.trekme.core.billing.data.model.SubscriptionType
import com.peterlaurence.trekme.core.billing.data.model.acknowledge
import com.peterlaurence.trekme.core.billing.data.model.assureAcknowledgement
import com.peterlaurence.trekme.core.billing.data.model.containsOneTimeOrSub
import com.peterlaurence.trekme.core.billing.data.model.getDetailsById
import com.peterlaurence.trekme.core.billing.data.model.toSubscriptionDetails
import com.peterlaurence.trekme.core.billing.domain.api.BillingApi
import com.peterlaurence.trekme.core.billing.domain.model.AccessGranted
import com.peterlaurence.trekme.core.billing.domain.model.GetSubscriptionDetailsFailure
import com.peterlaurence.trekme.core.billing.domain.model.PurchaseVerifier
import com.peterlaurence.trekme.core.billing.domain.model.SubscriptionDetails
import com.peterlaurence.trekme.events.AppEventBus
import com.peterlaurence.trekme.util.datetime.Days
import com.peterlaurence.trekme.util.datetime.days_
import com.peterlaurence.trekme.util.datetime.millis
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import java.util.Date

/**
 * Manages a subscription along with a one-time purchase.
 * To access some functionality, a user should have an active subscription, or a valid one-time
 * purchase.
 *
 * @since 2019/08/06
 */
class Billing<in T : SubscriptionType>(
    val application: Application,
    private val purchaseIds: PurchaseIdsContract,
    private val purchaseIdsResolver: PurchaseIdsResolver<T>,
    private val purchaseVerifier: PurchaseVerifier,
    private val appEventBus: AppEventBus,
) : BillingApi<T> {

    override val purchaseAcknowledgedEvent =
        MutableSharedFlow<Unit>(0, 1, BufferOverflow.DROP_OLDEST)

    /**
     * seems to stay uninitialized, and thus unused...
     */
    private lateinit var purchasePendingCallback: () -> Unit

    private val purchaseUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (purchases != null && billingResult.responseCode == OK) {
            purchases.forEach { purchase ->
                if (purchase.containsOneTimeOrSub(purchaseIds)) {
                    purchase.acknowledge(
                        acknowledgePurchaseFunctor,
                        onSuccess = { purchaseAcknowledgedEvent.tryEmit(Unit) },
                        onPending = { callPurchasePendingCallback() }
                    )
                }
            }
        }
    }

    private val subscriptionToProductMap =
        mutableMapOf<SubscriptionDetails, ProductDetails>()

    private val billingClient: BillingClient = BillingClient
        .newBuilder(application)
        .setListener(purchaseUpdatedListener)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        ).build()

    private val connector = BillingConnector(billingClient)

    private val query = BillingQuery(billingClient, purchaseIds)

    private val acknowledgePurchaseFunctor: AcknowledgePurchaseFunctor = query::acknowledgePurchase

    private suspend fun connect() = connector.connect()

    /**
     * This is one of the first things to do. If either the one-time or the subscription are among
     * the purchases, check if it should be acknowledged. This call is required when the
     * acknowledgement wasn't done right after a billing flow (typically when the payment method is
     * slow and the user didn't wait the end of the procedure with the [purchaseUpdatedListener] call).
     * So we can end up with a purchase which is in [Purchase.PurchaseState.PURCHASED] state but not
     * acknowledged.
     *
     * @return whether acknowledgment was done or not.
     */
    override suspend fun queryAndAcknowledgePurchases(): Boolean {
        if (!connect()) return false

        val oneTimeAcknowledged =
            query.queryPurchase(PurchaseType.ONE_TIME)
                ?.assureAcknowledgement(acknowledgePurchaseFunctor) ?: false

        val subAcknowledged =
            query.queryPurchase(PurchaseType.SUB)
                ?.assureAcknowledgement(acknowledgePurchaseFunctor) ?: false

        return oneTimeAcknowledged || subAcknowledged
    }

    /**
     * Also has a side effect of consuming not granted one time licenses...
     */
    override suspend fun queryPurchasesBeingPurchased(): Boolean {
        if (!connect()) return false

        val oneTimeLicense =
            query.queryPurchase(PurchaseType.VALID_ONE_TIME)?.run {
                if (
                    purchaseVerifier.checkTime(
                        purchaseTime.millis,
                        Date().time.millis
                    ) !is AccessGranted
                ) {
                    consume(this)
                    null
                } else this
            }

        return if (oneTimeLicense == null) {
            query.queryPurchase(PurchaseType.VALID_SUB) != null
        } else true
    }

    /**
     * Get the details of a subscription.
     */
    context(Raise<GetSubscriptionDetailsFailure>)
    override suspend fun getSubscriptionDetails(subscriptionType: T): SubscriptionDetails {

        if (!connect()) {
            raise(GetSubscriptionDetailsFailure.UnableToConnectToBilling)
        }

        val subId = purchaseIdsResolver(subscriptionType)
        val result = query.queryProductDetailsResult(subId)

        return when (result.billingResult.responseCode) {
            OK -> {
                result.getDetailsById(subId)?.let { productDetails ->
                    productDetails.toSubscriptionDetails(::parseTrialPeriodInDays).also {
                        subscriptionToProductMap[it] = productDetails
                    }
                } ?: raise(GetSubscriptionDetailsFailure.ProductNotFound(subId))
            }

            FEATURE_NOT_SUPPORTED -> raise(GetSubscriptionDetailsFailure.FeatureNotSupported)
            SERVICE_DISCONNECTED -> raise(GetSubscriptionDetailsFailure.ServiceDisconnected)
            else -> raise(GetSubscriptionDetailsFailure.OtherFailure)
        }
    }

    // TODO - continue with refactoring
    override fun launchBilling(
        subscription: SubscriptionDetails,
        onPurchasePending: () -> Unit,
    ) {
        val productDetails = subscriptionToProductMap[subscription] ?: return
        val offerToken = productDetails.subscriptionOfferDetails?.get(0)?.offerToken ?: return

        val productDetailsParamsList =
            listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)
                    .setOfferToken(offerToken)
                    .build()
            )
        val flowParams =
            BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build()

        val billingParams = BillingParams(billingClient, flowParams)

        /* Since we need an Activity to start the billing flow, we send an event which the activity
         * is listening */
        appEventBus.startBillingFlow(billingParams)
    }

    /**
     * Trial periods are given in the form "P1W" -> 1 week, or "P4D" -> 4 days.
     */
    private fun parseTrialPeriodInDays(period: String): Days {

        if (period.isEmpty()) return 0.days_

        val qty = period.filter { it.isDigit() }.toInt()

        return when (period.lowercase().last()) {
            'w' -> qty * 7
            'd' -> qty
            else -> qty
        }.days_
    }

    private fun callPurchasePendingCallback() {
        if (::purchasePendingCallback.isInitialized) {
            purchasePendingCallback()
        }
    }

    private fun consume(purchase: Purchase) {
        val params = ConsumeParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient.consumeAsync(params) { _, _ ->
            Log.i(TAG, "Consumed the purchase. It can now be bought again.")
        }
    }

}

private const val TAG = "Billing.kt"
