package com.peterlaurence.trekme.core.billing.domain.model

import com.peterlaurence.trekme.util.datetime.Days
import java.util.UUID

data class SubscriptionDetails(
    val id: UUID = UUID.randomUUID(),
    val price: String,
    val trialInfo: TrialInfo,
)

sealed interface TrialInfo
data class TrialAvailable(val duration: Days) : TrialInfo
data object TrialUnavailable : TrialInfo
