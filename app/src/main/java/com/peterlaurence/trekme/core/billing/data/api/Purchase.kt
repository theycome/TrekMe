package com.peterlaurence.trekme.core.billing.data.api

import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode.OK
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Created by Ivan Yakushev on 24.10.2024
 */
fun Purchase.acknowledge(
    client: BillingClient,
    onSuccess: (BillingResult) -> Unit,
    onPending: () -> Unit,
) {
    if (purchaseState == Purchase.PurchaseState.PURCHASED && !isAcknowledged) {
        acknowledgeByBilling(client) {
            if (it.responseCode == OK) {
                onSuccess(it)
            }
        }
    } else if (purchaseState == Purchase.PurchaseState.PENDING) {
        onPending()
    }
}

/**
 * Using a [callbackFlow] instead of [suspendCancellableCoroutine], as we have no way to remove
 * the provided callback given to [BillingClient.acknowledgePurchase] - so creating a memory
 * leak.
 * By collecting a [callbackFlow], the real collector is on a different call stack. So the
 * [BillingClient] has no reference on the collector.
 */
suspend fun Purchase.acknowledgeByBillingSuspended(client: BillingClient) = callbackFlow {
    acknowledgeByBilling(client) {
        trySend(it.responseCode == OK)
    }
    awaitClose { /* We can't do anything, but it doesn't matter */ }
}.first()

fun Purchase.acknowledgeByBilling(
    client: BillingClient,
    onSuccess: (BillingResult) -> Unit,
) {
    val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
        .setPurchaseToken(purchaseToken)
        .build()

    client.acknowledgePurchase(acknowledgePurchaseParams) {
        onSuccess(it)
    }
}

fun Purchase.containsOneTime(purchaseIds: PurchaseIds): Boolean =
    products.any { id -> purchaseIds.containsOneTime(id) }

fun Purchase.containsSub(purchaseIds: PurchaseIds): Boolean =
    products.any { id -> purchaseIds.containsSub(id) }

val Purchase.purchasedButNotAcknowledged: Boolean
    get() = purchaseState == Purchase.PurchaseState.PURCHASED && !isAcknowledged

// TODO - rename as queryWith
class OneTimePurchase(val purchase: Purchase) {
    companion object {
        fun from(queryResult: PurchasesQueriedResult, purchaseIds: PurchaseIds): OneTimePurchase? =
            queryResult.purchases.firstOrNull { it.containsOneTime(purchaseIds) }?.let {
                OneTimePurchase(it)
            }
    }
}

class SubPurchase(val purchase: Purchase) {
    companion object {
        fun from(queryResult: PurchasesQueriedResult, purchaseIds: PurchaseIds): SubPurchase? =
            queryResult.purchases.firstOrNull { it.containsSub(purchaseIds) }?.let {
                SubPurchase(it)
            }
    }
}

class ValidOneTimePurchase(val purchase: Purchase) {
    companion object {
        fun from(
            queryResult: PurchasesQueriedResult,
            purchaseIds: PurchaseIds,
        ): ValidOneTimePurchase? =
            queryResult.purchases.firstOrNull { it.containsOneTime(purchaseIds) && it.isAcknowledged }
                ?.let {
                    ValidOneTimePurchase(it)
                }
    }
}

class ValidSubPurchase(val purchase: Purchase) {
    companion object {
        fun from(queryResult: PurchasesQueriedResult, purchaseIds: PurchaseIds): ValidSubPurchase? =
            queryResult.purchases.firstOrNull { it.containsSub(purchaseIds) && it.isAcknowledged }
                ?.let {
                    ValidSubPurchase(it)
                }
    }
}
