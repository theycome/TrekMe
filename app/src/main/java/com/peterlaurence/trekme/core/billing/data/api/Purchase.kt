package com.peterlaurence.trekme.core.billing.data.api

import com.android.billingclient.api.BillingClient.BillingResponseCode.OK
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.peterlaurence.trekme.util.callbackFlowWrapper

/**
 * Created by Ivan Yakushev on 24.10.2024
 */
fun Purchase.acknowledge(
    billing: Billing,
    onSuccess: (BillingResult) -> Unit,
    onPending: () -> Unit,
) {
    if (purchaseState == Purchase.PurchaseState.PURCHASED && !isAcknowledged) {
        acknowledgeByBilling(billing) {
            if (it.responseCode == OK) {
                onSuccess(it)
            }
        }
    } else if (purchaseState == Purchase.PurchaseState.PENDING) {
        onPending()
    }
}

suspend fun Purchase.assureAcknowledgement(billing: Billing): Boolean =
    if (purchasedButNotAcknowledged) {
        acknowledgeByBillingSuspended(billing)
    } else false

fun Purchase.containsOneTime(purchaseIds: PurchaseIds): Boolean =
    products.any { id -> purchaseIds.containsOneTime(id) }

fun Purchase.containsSub(purchaseIds: PurchaseIds): Boolean =
    products.any { id -> purchaseIds.containsSub(id) }

fun Purchase.containsOneTimeOrSub(purchaseIds: PurchaseIds): Boolean =
    products.any { id -> purchaseIds.containsOneTime(id) || purchaseIds.containsSub(id) }

val Purchase.purchasedButNotAcknowledged: Boolean
    get() = purchaseState == Purchase.PurchaseState.PURCHASED && !isAcknowledged

private suspend fun Purchase.acknowledgeByBillingSuspended(billing: Billing): Boolean =
    callbackFlowWrapper { emit ->
        acknowledgeByBilling(billing) {
            emit {
                it.responseCode == OK
            }
        }
    }()

private fun Purchase.acknowledgeByBilling(
    billing: Billing,
    onSuccess: (BillingResult) -> Unit,
) = billing.acknowledge(this, onSuccess)

private typealias PurchaseComparator = (Purchase, PurchaseIds) -> Boolean

enum class PurchaseType(
    val productType: String,
    val comparator: PurchaseComparator,
) {

    ONE_TIME(ProductType.INAPP, Purchase::containsOneTime),

    VALID_ONE_TIME(ProductType.INAPP, { purchase, ids ->
        purchase.containsOneTime(ids) && purchase.isAcknowledged
    }),

    SUB(ProductType.SUBS, Purchase::containsSub),
    
    VALID_SUB(ProductType.SUBS, { purchase, ids ->
        purchase.containsSub(ids) && purchase.isAcknowledged
    })

}
