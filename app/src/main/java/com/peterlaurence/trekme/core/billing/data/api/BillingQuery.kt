package com.peterlaurence.trekme.core.billing.data.api

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryPurchasesParams
import com.peterlaurence.trekme.util.callbackFlowWrapper

/**
 * Created by Ivan Yakushev on 14.11.2024
 */
class BillingQuery(
    private val billingClient: BillingClient,
    private val purchaseIds: PurchaseIds,
) {

    suspend fun queryPurchase(type: PurchaseType): Purchase? =
        queryPurchasesResult(type)
            .getPurchase(type, purchaseIds)

    private suspend fun queryPurchasesResult(type: PurchaseType): PurchasesQueriedResult {

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(type.productType)
            .build()

        return callbackFlowWrapper { emit ->
            billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
                emit {
                    PurchasesQueriedResult(billingResult, purchases)
                }
            }
        }()
    }

}
