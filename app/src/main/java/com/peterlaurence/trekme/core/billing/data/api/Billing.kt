package com.peterlaurence.trekme.core.billing.data.api

import android.app.Application
import android.util.Log
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
import com.android.billingclient.api.QueryProductDetailsParams
import com.peterlaurence.trekme.core.billing.data.model.BillingParams
import com.peterlaurence.trekme.core.billing.domain.api.BillingApi
import com.peterlaurence.trekme.core.billing.domain.model.AccessGranted
import com.peterlaurence.trekme.core.billing.domain.model.NotSupportedException
import com.peterlaurence.trekme.core.billing.domain.model.ProductNotFoundException
import com.peterlaurence.trekme.core.billing.domain.model.PurchaseVerifier
import com.peterlaurence.trekme.core.billing.domain.model.SubscriptionDetails
import com.peterlaurence.trekme.core.billing.domain.model.TrialAvailable
import com.peterlaurence.trekme.core.billing.domain.model.TrialUnavailable
import com.peterlaurence.trekme.events.AppEventBus
import com.peterlaurence.trekme.util.datetime.millis
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Date
import java.util.UUID

/**
 * Manages a subscription along with a one-time purchase.
 * To access some functionality, a user should have an active subscription, or a valid one-time
 * purchase.
 *
 * @since 2019/08/06
 */
class Billing(
    val application: Application,
    private val purchaseIds: PurchaseIds,
    private val purchaseVerifier: PurchaseVerifier,
    private val appEventBus: AppEventBus,
) : BillingApi {

    // FIXME - encapsulate calls in a BillingClientWrapper

    override val purchaseAcknowledgedEvent =
        MutableSharedFlow<Unit>(0, 1, BufferOverflow.DROP_OLDEST)

    /**
     * seems to stay uninitialized...
     */
    private lateinit var purchasePendingCallback: () -> Unit

    private val purchaseUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (purchases != null && billingResult.responseCode == OK) {
            purchases.forEach { purchase ->
                if (purchase.products.any { id -> id == purchaseIds.oneTimeId || id in purchaseIds.subIdList }) {
                    purchase.acknowledge(
                        billingClient,
                        onSuccess = { purchaseAcknowledgedEvent.tryEmit(Unit) },
                        onPending = { callPurchasePendingCallback() }
                    )
                }
            }
        }
    }

    private val productDetailsForId = mutableMapOf<UUID, ProductDetails>()

    private val billingClient: BillingClient = BillingClient
        .newBuilder(application)
        .setListener(purchaseUpdatedListener)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        ).build()

    private val connector = BillingConnector(billingClient)

    private val wrapper = BillingClientWrapper(
        application,
        purchaseIds,
        onPurchaseSuccess = { purchaseAcknowledgedEvent.tryEmit(Unit) },
        onPurchasePending = { callPurchasePendingCallback() },
    )

    private fun callPurchasePendingCallback() {
        if (::purchasePendingCallback.isInitialized) {
            purchasePendingCallback()
        }
    }

    /**
     * This is one of the first things to do. If either the one-time or the subscription are among
     * the purchases, check if it should be acknowledged. This call is required when the
     * acknowledgement wasn't done right after a billing flow (typically when the payment method is
     * slow and the user didn't wait the end of the procedure with the [purchaseUpdatedListener] call).
     * So we can end up with a purchase which is in [Purchase.PurchaseState.PURCHASED] state but not
     * acknowledged.
     *
     * @return Whether acknowledgment was done or not.
     */
    override suspend fun acknowledgePurchase(): Boolean {
        if (!connector.connect()) return false

        val oneTimeAcknowledged =
            wrapper
                .queryInAppPurchases()
                .getPurchase(PurchaseType.ONE_TIME, purchaseIds)
                ?.assureAcknowledgement(billingClient) ?: false

        val subAcknowledged =
            wrapper
                .querySubPurchases()
                .getPurchase(PurchaseType.SUB, purchaseIds)
                ?.assureAcknowledgement(billingClient) ?: false

        return oneTimeAcknowledged || subAcknowledged
    }

    /**
     * Also has a side effect of consuming not granted one time licenses...
     */
    override suspend fun isPurchased(): Boolean {
        if (!connector.connect()) return false

        val oneTimeLicense = wrapper
            .queryInAppPurchases()
            .getPurchase(PurchaseType.VALID_ONE_TIME, purchaseIds)?.run {
                if (purchaseVerifier
                        .checkTime(purchaseTime.millis, Date().time.millis) !is AccessGranted
                ) {
                    consume(purchaseToken)
                    null
                } else this
            }

        return if (oneTimeLicense == null) {
            wrapper.querySubPurchases()
                .getPurchase(PurchaseType.VALID_SUB, purchaseIds) != null
        } else true
    }

    private fun consume(token: String) {
        // FIXME - HERE
        val consumeParams = ConsumeParams.newBuilder().setPurchaseToken(token).build()
        billingClient.consumeAsync(consumeParams) { _, _ ->
            Log.i(TAG, "Consumed the purchase. It can now be bought again.")
        }
    }

    /**
     * Get the details of a subscription.
     * @throws [ProductNotFoundException], [NotSupportedException], [IllegalStateException]
     */
    // FIXME - use typed result as a return type instead of exceptions
    override suspend fun getSubDetails(index: Int): SubscriptionDetails {
        val subId = purchaseIds.subIdList.getOrNull(index) ?: error("no sku for index $index")

        if (!connector.connect()) error("failed to connect to billing")

        val (billingResult, skuDetailsList) = querySubDetails(subId)
        return when (billingResult.responseCode) {
            OK -> skuDetailsList.find { it.productId == subId }?.let {
                makeSubscriptionDetails(it)
            } ?: throw ProductNotFoundException()

            FEATURE_NOT_SUPPORTED -> throw NotSupportedException()
            SERVICE_DISCONNECTED -> error("should retry")
            else -> error("other error")
        }
    }

    /**
     * Using a [callbackFlow] instead of [suspendCancellableCoroutine], as we have no way to remove
     * the provided callback given to [BillingClient.queryPurchasesAsync] - so creating a memory
     * leak.
     * By collecting a [callbackFlow], the real collector is on a different call stack. So the
     * [BillingClient] has no reference on the collector.
     */
    private suspend fun querySubDetails(subId: String): ProductDetailsResult = callbackFlow {
        val productList =
            listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(subId)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            )

        val params = QueryProductDetailsParams.newBuilder().setProductList(productList)
        billingClient.queryProductDetailsAsync(params.build()) {
                billingResult,
                productDetailsList,
            ->
            trySend(ProductDetailsResult(billingResult, productDetailsList))
        }

        awaitClose { /* We can't do anything, but it doesn't matter */ }
    }.first()

    override fun launchBilling(
        id: UUID,
        purchasePendingCb: () -> Unit,
    ) {
        val productDetails = productDetailsForId[id] ?: return
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

    private fun makeSubscriptionDetails(productDetails: ProductDetails): SubscriptionDetails? {
        /**
         * Trial periods are given in the form "P1W" -> 1 week, or "P4D" -> 4 days.
         */
        fun parseTrialPeriodInDays(period: String): Int {
            if (period.isEmpty()) return 0
            val qty = period.filter { it.isDigit() }.toInt()
            return when (period.lowercase().last()) {
                'w' -> qty * 7
                'd' -> qty
                else -> qty
            }
        }

        /* Assign an id and remember it (needed for purchase) */
        val id = UUID.randomUUID()
        productDetailsForId[id] = productDetails

        /* For the moment, we only support the base plan */
        val offer = productDetails.subscriptionOfferDetails?.firstOrNull() ?: return null

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
        } ?: return null

        return SubscriptionDetails(
            id = id,
            price = realPricePhase.formattedPrice,
            trialInfo = trialInfo
        )
    }

    private data class ProductDetailsResult(
        val billingResult: BillingResult,
        val productDetailsList: List<ProductDetails>,
    )

}

/**
 * A wrapper around data returned by [BillingClient.queryPurchasesAsync]
 */
data class PurchasesQueriedResult(
    val billingResult: BillingResult,
    val purchases: List<Purchase>,
)

fun PurchasesQueriedResult.getPurchase(type: PurchaseType, purchaseIds: PurchaseIds): Purchase? =
    purchases.firstOrNull {
        type.comparator(it, purchaseIds)
    }

private const val TAG = "Billing.kt"
