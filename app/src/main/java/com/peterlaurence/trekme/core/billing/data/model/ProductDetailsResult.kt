package com.peterlaurence.trekme.core.billing.data.model

import arrow.core.raise.Raise
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.peterlaurence.trekme.core.billing.domain.model.GetSubscriptionDetailsFailure
import com.peterlaurence.trekme.core.billing.domain.model.SubscriptionDetails
import com.peterlaurence.trekme.core.billing.domain.model.TrialAvailable
import com.peterlaurence.trekme.core.billing.domain.model.TrialUnavailable

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

context(Raise<GetSubscriptionDetailsFailure>)
fun ProductDetails.toSubscriptionDetails(trialParser: (String) -> Int): SubscriptionDetails {

    /* For the moment, we only support the base plan */
    val offer = subscriptionOfferDetails?.firstOrNull()
        ?: raise(GetSubscriptionDetailsFailure.OnlyBasePlanSupported)

    /* The trial is the first pricing phase with 0 as price amount */
    // TODO 1.) - extract into function
    val trialData =
        offer.pricingPhases.pricingPhaseList.firstOrNull { it.priceAmountMicros == 0L }
    val trialInfo = if (trialData == null) {
        TrialUnavailable
    } else {
        // TODO - typify parameter as Days
        TrialAvailable(trialDurationInDays = trialParser(trialData.billingPeriod))
    }

    /* The "real" price phase is the first phase with a price other than 0 */
    // TODO 1.)
    val realPricePhase = offer.pricingPhases.pricingPhaseList.firstOrNull {
        it.priceAmountMicros != 0L
    } ?: raise(GetSubscriptionDetailsFailure.IncorrectPricingPhaseFound)

    return SubscriptionDetails(
        price = realPricePhase.formattedPrice,
        trialInfo = trialInfo,
    )
}
