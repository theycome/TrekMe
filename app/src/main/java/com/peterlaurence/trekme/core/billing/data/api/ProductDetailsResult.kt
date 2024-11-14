package com.peterlaurence.trekme.core.billing.data.api

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
