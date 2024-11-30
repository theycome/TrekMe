package com.peterlaurence.trekme.core.billing.data.api

import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.peterlaurence.trekme.core.billing.data.model.ProductDetailsResult
import com.peterlaurence.trekme.core.billing.data.model.PurchaseIdsContract
import com.peterlaurence.trekme.core.billing.data.model.PurchaseType
import com.peterlaurence.trekme.core.billing.data.model.PurchasesResult
import com.peterlaurence.trekme.core.billing.data.model.getPurchase
import com.peterlaurence.trekme.util.callbackFlowWrapper
import com.peterlaurence.trekme.util.log

/**
 * Created by Ivan Yakushev on 14.11.2024
 */
class BillingQuery(
    private val billingClient: BillingClient,
    private val purchaseIds: PurchaseIdsContract,
) {

    suspend fun queryPurchase(type: PurchaseType): Purchase? =
        queryPurchasesResult(type)
            .getPurchase(type, purchaseIds)

    suspend fun queryProductDetailsResult(subId: String): ProductDetailsResult {

        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(subId)
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(product))
            .build()

        return callbackFlowWrapper { emit ->
            billingClient.queryProductDetailsAsync(params) { billingResult, productDetails ->
                emit {
                    ProductDetailsResult(billingResult, productDetails)
                }
            }
        }()
    }

    fun acknowledgePurchase(
        purchase: Purchase,
        onSuccess: (BillingResult) -> Unit,
    ) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient.acknowledgePurchase(params) {
            onSuccess(it)
        }
    }

    fun consume(purchase: Purchase) {
        val params = ConsumeParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient.consumeAsync(params) { _, _ ->
            log("Consumed the purchase. It can now be bought again.")
        }
    }

    private suspend fun queryPurchasesResult(type: PurchaseType): PurchasesResult {

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(type.productType)
            .build()

        return callbackFlowWrapper { emit ->
            billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
                emit {
                    PurchasesResult(billingResult, purchases)
                }
            }
        }()
    }

}

typealias AcknowledgePurchaseFunctor = (Purchase, (BillingResult) -> Unit) -> Unit
