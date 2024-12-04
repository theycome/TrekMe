package com.peterlaurence.trekme.core.billing.domain.model

import com.peterlaurence.trekme.util.datetime.Millis

interface PurchaseVerifier {
    fun checkTime(purchaseTime: Millis, now: Millis): AccessState
}

sealed class AccessState
data class AccessGranted(val remainingDays: Int) : AccessState()
data class GracePeriod(val remainingDays: Int) : AccessState()
data object AccessDeniedLicenseOutdated : AccessState()
