package com.peterlaurence.trekme.core.billing.domain.model

import com.peterlaurence.trekme.util.CallbackFlowFailure

sealed class GetSubscriptionDetailsFailure {
    data class ProductNotFound(val subscriptionId: String) : GetSubscriptionDetailsFailure()
    data class DurationParsingFailed(val returnedByApi: String) : GetSubscriptionDetailsFailure()
    data class InCallbackFlow(val failure: CallbackFlowFailure) :
        GetSubscriptionDetailsFailure()

    data object UnableToConnectToBilling : GetSubscriptionDetailsFailure()
    data object FeatureNotSupported : GetSubscriptionDetailsFailure()
    data object ServiceDisconnected : GetSubscriptionDetailsFailure()
    data object OtherFailure : GetSubscriptionDetailsFailure()
    data object OnlyBasePlanSupported : GetSubscriptionDetailsFailure()
    data object IncorrectPricingPhaseFound : GetSubscriptionDetailsFailure()
}
