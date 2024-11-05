package com.peterlaurence.trekme.core.billing.data.api

import android.app.Application
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode.OK
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryPurchasesParams
import com.peterlaurence.trekme.util.datetime.Millis
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Created by Ivan Yakushev on 03.11.2024
 *
 * Isolate all calls into Billing api through this class
 */
class BillingClientWrapper(
    application: Application,
    private val purchaseIds: PurchaseIds,
    private val onPurchaseSuccess: () -> Unit,
    private val onPurchasePending: () -> Unit,
) {

    private val purchaseUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (purchases != null && billingResult.responseCode == OK) {
            purchases.forEach { purchase ->
                if (purchase.containsOneTimeOrSub(purchaseIds)) {
                    purchase.acknowledge(
                        client,
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

    suspend fun queryInAppPurchases(): PurchasesQueriedResult {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        return queryPurchases(params)
    }

    suspend fun querySubPurchases(): PurchasesQueriedResult {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        return queryPurchases(params)
    }

    /**
     * Using a [callbackFlow] instead of [suspendCancellableCoroutine], as we have no way to remove
     * the provided callback given to [BillingClient.queryPurchasesAsync] - so creating a memory
     * leak.
     * By collecting a [callbackFlow], the real collector is on a different call stack. So the
     * [BillingClient] has no reference on the collector.
     */
    private suspend fun queryPurchases(params: QueryPurchasesParams): PurchasesQueriedResult =
        callbackFlow {
            client.queryPurchasesAsync(params) { billingResult, purchases ->
                trySend(PurchasesQueriedResult(billingResult, purchases))
            }
            awaitClose { /* We can't do anything, but it doesn't matter */ }
        }.first()

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
