package com.peterlaurence.trekme.core.billing.data.api

import com.android.billingclient.api.BillingClient.BillingResponseCode.OK
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.peterlaurence.trekme.util.callbackFlowWrapper

/**
 * Created by Ivan Yakushev on 24.10.2024
 */
fun Purchase.acknowledge(
    billingWrapper: BillingClientWrapper,
    onSuccess: (BillingResult) -> Unit,
    onPending: () -> Unit,
) {
    if (purchaseState == Purchase.PurchaseState.PURCHASED && !isAcknowledged) {
        acknowledgeByBilling(billingWrapper) {
            if (it.responseCode == OK) {
                onSuccess(it)
            }
        }
    } else if (purchaseState == Purchase.PurchaseState.PENDING) {
        onPending()
    }
}

suspend fun Purchase.assureAcknowledgement(billingWrapper: BillingClientWrapper): Boolean =
    if (purchasedButNotAcknowledged) {
        acknowledgeByBillingSuspended(billingWrapper)
    } else false

fun Purchase.containsOneTime(purchaseIds: PurchaseIds): Boolean =
    products.any { id -> purchaseIds.containsOneTime(id) }

fun Purchase.containsSub(purchaseIds: PurchaseIds): Boolean =
    products.any { id -> purchaseIds.containsSub(id) }

fun Purchase.containsOneTimeOrSub(purchaseIds: PurchaseIds): Boolean =
    products.any { id -> purchaseIds.containsOneTime(id) || purchaseIds.containsSub(id) }

val Purchase.purchasedButNotAcknowledged: Boolean
    get() = purchaseState == Purchase.PurchaseState.PURCHASED && !isAcknowledged

private suspend fun Purchase.acknowledgeByBillingSuspended(billingWrapper: BillingClientWrapper): Boolean =
    callbackFlowWrapper { emit ->
        acknowledgeByBilling(billingWrapper) {
            emit {
                it.responseCode == OK
            }
        }
    }()

private fun Purchase.acknowledgeByBilling(
    billingWrapper: BillingClientWrapper,
    onSuccess: (BillingResult) -> Unit,
) = billingWrapper.acknowledgePurchase(this, onSuccess)

private typealias PurchaseComparator = (Purchase, PurchaseIds) -> Boolean

enum class PurchaseType(val comparator: PurchaseComparator) {

    ONE_TIME(Purchase::containsOneTime),

    SUB(Purchase::containsSub),

    VALID_ONE_TIME({ purchase, ids ->
        purchase.containsOneTime(ids) && purchase.isAcknowledged
    }),

    VALID_SUB({ purchase, ids ->
        purchase.containsSub(ids) && purchase.isAcknowledged
    })

}
