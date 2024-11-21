package com.peterlaurence.trekme.core.billing.data.api

import android.app.Application
import android.util.Log
import arrow.core.raise.Raise
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED
import com.android.billingclient.api.BillingClient.BillingResponseCode.OK
import com.android.billingclient.api.BillingClient.BillingResponseCode.SERVICE_DISCONNECTED
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.peterlaurence.trekme.core.billing.data.model.BillingParams
import com.peterlaurence.trekme.core.billing.data.model.PurchaseIds
import com.peterlaurence.trekme.core.billing.data.model.PurchaseType
import com.peterlaurence.trekme.core.billing.data.model.SubscriptionType
import com.peterlaurence.trekme.core.billing.data.model.acknowledge
import com.peterlaurence.trekme.core.billing.data.model.assureAcknowledgement
import com.peterlaurence.trekme.core.billing.data.model.containsOneTimeOrSub
import com.peterlaurence.trekme.core.billing.domain.api.BillingApi
import com.peterlaurence.trekme.core.billing.domain.model.AccessGranted
import com.peterlaurence.trekme.core.billing.domain.model.GetSubscriptionDetailsFailure
import com.peterlaurence.trekme.core.billing.domain.model.PurchaseVerifier
import com.peterlaurence.trekme.core.billing.domain.model.SubscriptionDetails
import com.peterlaurence.trekme.core.billing.domain.model.TrialAvailable
import com.peterlaurence.trekme.core.billing.domain.model.TrialUnavailable
import com.peterlaurence.trekme.events.AppEventBus
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
    private val purchaseIds: PurchaseIds,
    private val purchaseVerifier: PurchaseVerifier,
    private val appEventBus: AppEventBus,
) : BillingApi<T> {

    override val purchaseAcknowledgedEvent =
        MutableSharedFlow<Unit>(0, 1, BufferOverflow.DROP_OLDEST)

    /**
     * seems to stay uninitialized...
     */
    private lateinit var purchasePendingCallback: () -> Unit

    private val purchaseUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (purchases != null && billingResult.responseCode == OK) {
            purchases.forEach { purchase ->
                if (purchase.containsOneTimeOrSub(purchaseIds)) {
                    purchase.acknowledge(
                        this,
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
    override suspend fun acknowledgePurchase(): Boolean {
        if (!connect()) return false

        val oneTimeAcknowledged =
            query.queryPurchase(PurchaseType.ONE_TIME)
                ?.assureAcknowledgement(this) ?: false

        val subAcknowledged =
            query.queryPurchase(PurchaseType.SUB)
                ?.assureAcknowledgement(this) ?: false

        return oneTimeAcknowledged || subAcknowledged
    }

    /**
     * Also has a side effect of consuming not granted one time licenses...
     */
    override suspend fun isPurchased(): Boolean {
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

        // TODO - add dispatcher code into PurchaseIds

//        when (subscriptionType) {
//            SubscriptionType.Single -> {}
//            SubscriptionType.MonthAndYear.Month -> {}
//            SubscriptionType.MonthAndYear.Year -> {}
//        }

//        val subId = purchaseIds.subIdList.getOrNull(index) ?: raise(
//            GetSubscriptionDetailsFailure.NoSkuFound(index)
//        )

        val subId = ""

        if (!connect()) {
            raise(GetSubscriptionDetailsFailure.UnableToConnectToBilling)
        }

        val (billingResult, skuDetailsList) =
            query.queryProductDetailsResult(subId)

        return when (billingResult.responseCode) {
            OK -> skuDetailsList.find { it.productId == subId }?.let {
                makeSubscriptionDetails(it)
            } ?: raise(GetSubscriptionDetailsFailure.ProductNotFound(subId))

            FEATURE_NOT_SUPPORTED -> raise(GetSubscriptionDetailsFailure.FeatureNotSupported)
            SERVICE_DISCONNECTED -> raise(GetSubscriptionDetailsFailure.ServiceDisconnected)
            else -> raise(GetSubscriptionDetailsFailure.OtherFailure)
        }
    }

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

    // TODO - shouldn't expose, must be private
    fun acknowledge(
        purchase: Purchase,
        onSuccess: (BillingResult) -> Unit,
    ) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient.acknowledgePurchase(params) {
            onSuccess(it)
        }
    }

    /**
     * Trial periods are given in the form "P1W" -> 1 week, or "P4D" -> 4 days.
     */
    private fun parseTrialPeriodInDays(period: String): Int {
        if (period.isEmpty()) return 0
        val qty = period.filter { it.isDigit() }.toInt()
        return when (period.lowercase().last()) {
            'w' -> qty * 7
            'd' -> qty
            else -> qty
        }
    }

    context(Raise<GetSubscriptionDetailsFailure>)
    private fun makeSubscriptionDetails(productDetails: ProductDetails): SubscriptionDetails {

        /* For the moment, we only support the base plan */
        val offer = productDetails.subscriptionOfferDetails?.firstOrNull()
            ?: raise(GetSubscriptionDetailsFailure.OnlyBasePlanSupported)

        /* The trial is the first pricing phase with 0 as price amount */
        val trialData =
            offer.pricingPhases.pricingPhaseList.firstOrNull { it.priceAmountMicros == 0L }
        val trialInfo = if (trialData != null) {
            TrialAvailable(trialDurationInDays = parseTrialPeriodInDays(trialData.billingPeriod))
        } else {
            TrialUnavailable
        }

        /* The "real" price phase is the first phase with a price other than 0 */
        val realPricePhase = offer.pricingPhases.pricingPhaseList.firstOrNull {
            it.priceAmountMicros != 0L
        } ?: raise(GetSubscriptionDetailsFailure.IncorrectPricingPhaseFound)

        return SubscriptionDetails(
            price = realPricePhase.formattedPrice,
            trialInfo = trialInfo,
        ).also {
            subscriptionToProductMap[it] = productDetails
        }
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
