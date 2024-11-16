package com.peterlaurence.trekme.core.billing.data.model

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails

/**
 * Created by Ivan Yakushev on 14.11.2024
 *
 * A wrapper around data returned by [BillingClient.queryProductDetailsAsync]
 */
data class ProductDetailsResult(
    val billingResult: BillingResult,
    val productDetails: List<ProductDetails>,
)

//fun ProductDetailsResult.foo(subId: String) {
//    return when (billingResult.responseCode) {
//        OK -> getDetailsById(subId)?.let {
////            makeSubscriptionDetails(it)
//        } ?: throw ProductNotFoundException()
//
//        FEATURE_NOT_SUPPORTED -> throw NotSupportedException()
//        SERVICE_DISCONNECTED -> error("should retry")
//        else -> error("other error")
//    }
//}

private fun ProductDetailsResult.getDetailsById(subId: String): ProductDetails? =
    productDetails.find { it.productId == subId }

//private fun ProductDetails.toSubscriptionDetails(): SubscriptionDetails? {
//}
