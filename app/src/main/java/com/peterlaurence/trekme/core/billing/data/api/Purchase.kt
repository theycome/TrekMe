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

// TODO - products.any { oneTime / sub } extract as a function

val Purchase.purchasedButNotAcknowledged: Boolean
    get() = purchaseState == Purchase.PurchaseState.PURCHASED && !isAcknowledged

fun Purchase.shouldAcknowledge(purchaseIds: PurchaseIds): Boolean =
    purchasedButNotAcknowledged &&
        products.any { id -> purchaseIds.containsOneTime(id) }

fun Purchase.shouldAcknowledgeSub(purchaseIds: PurchaseIds): Boolean =
    purchasedButNotAcknowledged &&
        (products.any { id -> purchaseIds.containsSub(id) })

// TODO - separate into two classes RegularPurchase, SubPurchase ?

typealias PurchaseList = List<Purchase>

fun PurchaseList.getOneTimePurchase(purchaseIds: PurchaseIds): Purchase? {
    return firstOrNull { it.products.any { id -> purchaseIds.containsOneTime(id) } }
}

fun PurchaseList.getSubPurchase(purchaseIds: PurchaseIds): Purchase? {
    return firstOrNull { it.products.any { id -> purchaseIds.containsSub(id) } }
}

fun PurchaseList.getValidOneTimePurchase(purchaseIds: PurchaseIds): Purchase? {
    return firstOrNull {
        it.products.any { id -> purchaseIds.containsOneTime(id) } &&
            it.isAcknowledged
    }
}

fun PurchaseList.getValidSubPurchase(purchaseIds: PurchaseIds): Purchase? {
    return firstOrNull {
        it.products.any { id -> purchaseIds.containsSub(id) } &&
            it.isAcknowledged
    }
}
