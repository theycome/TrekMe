package com.peterlaurence.trekme.core.billing.domain.model

sealed class GetSubscriptionDetailsFailure {
    data class NoSkuFound(val atIndex: Int) : GetSubscriptionDetailsFailure()
    data class ProductNotFound(val subscriptionId: String) : GetSubscriptionDetailsFailure()
    data object UnableToConnectToBilling : GetSubscriptionDetailsFailure()
    data object FeatureNotSupported : GetSubscriptionDetailsFailure()
    data object ServiceDisconnected : GetSubscriptionDetailsFailure()
    data object OtherFailure : GetSubscriptionDetailsFailure()
    data object OnlyBasePlanSupported : GetSubscriptionDetailsFailure()
    data object IncorrectPricingPhaseFound : GetSubscriptionDetailsFailure()
}
