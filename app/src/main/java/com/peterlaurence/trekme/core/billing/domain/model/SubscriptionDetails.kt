package com.peterlaurence.trekme.core.billing.domain.model

import arrow.core.raise.Raise
import com.android.billingclient.api.ProductDetails
import com.peterlaurence.trekme.core.billing.data.model.getPricingPhase
import java.util.UUID

data class SubscriptionDetails(
    val id: UUID = UUID.randomUUID(),
    val price: String,
    val trialInfo: TrialInfo,
) {

    companion object {

        context(Raise<GetSubscriptionDetailsFailure>)
        operator fun invoke(productDetails: ProductDetails): SubscriptionDetails {

            /* For the moment, we only support the base plan */
            val offer = productDetails
                .subscriptionOfferDetails?.firstOrNull()
                ?: raise(GetSubscriptionDetailsFailure.OnlyBasePlanSupported)

            /* The trial is the first pricing phase with 0 as price amount */
            val trialPricingPhase = offer.getPricingPhase { it == 0L }
            val trialInfo = trialPricingPhase?.billingPeriod?.let {
                TrialAvailable(it) ?: raise(GetSubscriptionDetailsFailure.DurationParsingFailed(it))
            } ?: TrialUnavailable

            /* The "real" price phase is the first phase with a price other than 0 */
            val realPricePhase = offer.getPricingPhase { it != 0L }
                ?: raise(GetSubscriptionDetailsFailure.IncorrectPricingPhaseFound)

            return SubscriptionDetails(
                price = realPricePhase.formattedPrice,
                trialInfo = trialInfo,
            )
        }

    }

}

context(Raise<GetSubscriptionDetailsFailure>)
fun ProductDetails.toSubscriptionDetails(): SubscriptionDetails =
    SubscriptionDetails(productDetails = this)
