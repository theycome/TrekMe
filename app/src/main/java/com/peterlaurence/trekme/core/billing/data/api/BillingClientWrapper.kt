package com.peterlaurence.trekme.core.billing.data.api

import android.app.Application
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode.OK
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Created by Ivan Yakushev on 03.11.2024
 *
 * Isolate all calls into Billing api into this class
 * TODO - also add Connector as a nested class Connector
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

    private val connector = BillingConnector(client)

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

}
