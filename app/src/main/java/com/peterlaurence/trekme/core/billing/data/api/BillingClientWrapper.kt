package com.peterlaurence.trekme.core.billing.data.api

import android.app.Application
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode.OK
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryPurchasesParams
import com.peterlaurence.trekme.core.billing.domain.model.AccessGranted
import com.peterlaurence.trekme.core.billing.domain.model.PurchaseVerifier
import com.peterlaurence.trekme.util.callbackFlowWrapper
import com.peterlaurence.trekme.util.datetime.Millis
import com.peterlaurence.trekme.util.datetime.millis
import kotlinx.coroutines.delay
import java.util.Date

/**
 * Created by Ivan Yakushev on 03.11.2024
 *
 * Isolate all calls into Billing api through this class
 */
class BillingClientWrapper(
    application: Application,
    private val purchaseIds: PurchaseIds,
    private val purchaseVerifier: PurchaseVerifier,
    private val onPurchaseSuccess: () -> Unit,
    private val onPurchasePending: () -> Unit,
) {

    private val purchaseUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (purchases != null && billingResult.responseCode == OK) {
            purchases.forEach { purchase ->
                if (purchase.containsOneTimeOrSub(purchaseIds)) {
                    purchase.acknowledge(
                        this,
                        onSuccess = { onPurchaseSuccess() },
                        onPending = { onPurchasePending() }
                    )
                }
            }
        }
    }

    private val client: BillingClient = BillingClient
        .newBuilder(application)
        .setListener(purchaseUpdatedListener)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        ).build()

    private val connector = Connector()

    suspend fun connect() = connector.connect()

    private suspend fun queryInAppPurchases(): PurchasesQueriedResult {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        return queryPurchases(params)
    }

    private suspend fun querySubPurchases(): PurchasesQueriedResult {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        return queryPurchases(params)
    }

    private suspend fun queryPurchases(params: QueryPurchasesParams): PurchasesQueriedResult =
        callbackFlowWrapper { emit ->
            client.queryPurchasesAsync(params) { billingResult, purchases ->
                emit {
                    PurchasesQueriedResult(billingResult, purchases)
                }
            }
        }()

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
    suspend fun acknowledgePurchase(): Boolean {
        if (!connect()) return false

        val oneTimeAcknowledged =
            queryInAppPurchases()
                .getPurchase(PurchaseType.ONE_TIME, purchaseIds)
                ?.assureAcknowledgement(this) ?: false

        val subAcknowledged =
            querySubPurchases()
                .getPurchase(PurchaseType.SUB, purchaseIds)
                ?.assureAcknowledgement(this) ?: false

        return oneTimeAcknowledged || subAcknowledged
    }

    fun acknowledge(
        purchase: Purchase,
        onSuccess: (BillingResult) -> Unit,
    ) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        client.acknowledgePurchase(params) {
            onSuccess(it)
        }
    }

    /**
     * Also has a side effect of consuming not granted one time licenses...
     */
    suspend fun isPurchased(): Boolean {
        if (!connect()) return false

        val oneTimeLicense =
            queryInAppPurchases()
                .getPurchase(PurchaseType.VALID_ONE_TIME, purchaseIds)?.run {
                    if (purchaseVerifier
                            .checkTime(purchaseTime.millis, Date().time.millis) !is AccessGranted
                    ) {
                        consume(this)
                        null
                    } else this
                }

        return if (oneTimeLicense == null) {
            querySubPurchases()
                .getPurchase(PurchaseType.VALID_SUB, purchaseIds) != null
        } else true
    }

    private fun consume(purchase: Purchase) {
        val params = ConsumeParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        client.consumeAsync(params) { _, _ ->
            Log.i(TAG, "Consumed the purchase. It can now be bought again.")
        }
    }

    /**
     * Encapsulates connection functionality
     */
    private inner class Connector {

        private var connected = false

        private val connectionStateListener = object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                connected = billingResult.responseCode == OK
            }

            override fun onBillingServiceDisconnected() {
                connected = false
            }
        }

        suspend fun connect(): Boolean =
            runCatching {
                awaitConnect(
                    TEN_SECONDS,
                    TEN_MILLIS,
                )
            }.isSuccess

        /**
         * Suspends at most 10s (waits for billing client to connect).
         * Since the [BillingClient] can only notify its state through the [connectionStateListener], we
         * poll the [connected] status. Ideally, we would collect the billing client state flow...
         */
        private suspend fun awaitConnect(
            totalWaitTime: Millis,
            delayTime: Millis,
        ) {

            connectClient()

            var awaited = Millis(0)

            while (awaited < totalWaitTime) {
                if (connected) {
                    break
                } else {
                    delay(delayTime.long)
                    awaited += delayTime
                }
            }

        }

        /**
         * Attempts to connect the billing service. This function immediately returns.
         * See also [awaitConnect], which suspends at most for 10s.
         * Don't try to make this a suspend function - the [client] keeps a reference on the
         * [BillingClientStateListener] so it would keep a reference on a continuation (leading to
         * insidious memory leaks, depending on who invokes that suspending function).
         * Done this way, we're sure that the [client] only has a reference on this [Billing]
         * instance.
         */
        private fun connectClient() {
            with(client) {
                if (isReady)
                    connected = true
                else
                    startConnection(connectionStateListener)
            }
        }

    }

    companion object {
        private val TEN_SECONDS = Millis(10_000)
        private val TEN_MILLIS = Millis(10)
    }

}

private const val TAG = "Billing.kt"
