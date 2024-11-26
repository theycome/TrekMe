package com.peterlaurence.trekme.core.billing.data.model

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.ProductDetails

data class BillingParams(val billingClient: BillingClient, val flowParams: BillingFlowParams) {

    companion object {

        operator fun invoke(
            billingClient: BillingClient,
            productDetails: ProductDetails,
            offerToken: String,
        ): BillingParams {

            val productDetailsParamsList =
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .setOfferToken(offerToken)
                        .build()
                )
            val flowParams =
                BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(productDetailsParamsList)
                    .build()

            return BillingParams(billingClient, flowParams)

        }

    }

}
