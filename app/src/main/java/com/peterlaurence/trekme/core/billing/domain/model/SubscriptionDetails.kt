package com.peterlaurence.trekme.core.billing.domain.model

import java.util.UUID

data class SubscriptionDetails(
    val id: UUID = UUID.randomUUID(),
    val price: String,
    val trialInfo: TrialInfo,
)

sealed interface TrialInfo
data class TrialAvailable(val trialDurationInDays: Int) : TrialInfo
object TrialUnavailable : TrialInfo
