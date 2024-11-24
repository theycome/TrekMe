package com.peterlaurence.trekme.core.billing.data.model

import com.android.billingclient.api.BillingClient.BillingResponseCode.OK
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.peterlaurence.trekme.core.billing.data.api.AcknowledgePurchaseFunctor
import com.peterlaurence.trekme.util.callbackFlowWrapper

/**
 * Created by Ivan Yakushev on 24.10.2024
 */
fun Purchase.acknowledge(
    acknowledgeFunctor: AcknowledgePurchaseFunctor,
    onSuccess: (BillingResult) -> Unit,
    onPending: () -> Unit,
) {
    if (purchaseState == Purchase.PurchaseState.PURCHASED && !isAcknowledged) {
        acknowledgeFunctor(this) {
            if (it.responseCode == OK) {
                onSuccess(it)
            }
        }
    } else if (purchaseState == Purchase.PurchaseState.PENDING) {
        onPending()
    }
}

suspend fun Purchase.assureAcknowledgement(acknowledgeFunctor: AcknowledgePurchaseFunctor): Boolean =
    if (purchasedButNotAcknowledged) {
        callbackFlowWrapper { emit ->
            acknowledgeFunctor(this) {
                emit {
                    it.responseCode == OK
                }
            }
        }()
    } else false

fun Purchase.containsOneTime(purchaseIds: PurchaseIdsContract): Boolean =
    products.any { id -> purchaseIds.containsOneTime(id) }

fun Purchase.containsSub(purchaseIds: PurchaseIdsContract): Boolean =
    products.any { id -> purchaseIds.containsSub(id) }

fun Purchase.containsOneTimeOrSub(purchaseIds: PurchaseIdsContract): Boolean =
    products.any { id -> purchaseIds.containsOneTime(id) || purchaseIds.containsSub(id) }

val Purchase.purchasedButNotAcknowledged: Boolean
    get() = purchaseState == Purchase.PurchaseState.PURCHASED && !isAcknowledged

private typealias PurchaseComparator = (Purchase, PurchaseIdsContract) -> Boolean

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
