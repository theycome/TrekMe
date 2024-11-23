package com.peterlaurence.trekme.core.billing.data.model

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase

/**
 * Created by Ivan Yakushev on 12.11.2024
 *
 * A wrapper around data returned by [BillingClient.queryPurchasesAsync]
 */
data class PurchasesResult(
    val billingResult: BillingResult,
    val purchases: List<Purchase>,
)

fun PurchasesResult.getPurchase(type: PurchaseType, purchaseIds: PurchaseIdsContract): Purchase? =
    purchases.firstOrNull {
        type.comparator(it, purchaseIds)
    }
