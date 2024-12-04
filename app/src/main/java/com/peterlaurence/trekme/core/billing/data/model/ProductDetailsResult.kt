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

fun ProductDetailsResult.getDetailsById(subId: String): ProductDetails? =
    productDetails.find { it.productId == subId }

fun ProductDetails.SubscriptionOfferDetails.getPricingPhase(comparator: (Long) -> Boolean) =
    pricingPhases.pricingPhaseList.firstOrNull { comparator(it.priceAmountMicros) }

fun ProductDetails.offerToken() =
    subscriptionOfferDetails?.get(0)?.offerToken
