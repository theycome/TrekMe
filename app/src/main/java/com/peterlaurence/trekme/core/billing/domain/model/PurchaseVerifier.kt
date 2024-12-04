package com.peterlaurence.trekme.core.billing.domain.model

import com.peterlaurence.trekme.util.datetime.Millis
import com.peterlaurence.trekme.util.datetime.millis
import java.util.Date

interface PurchaseVerifier {
    fun checkTime(purchaseTime: Millis, now: Millis = Date().time.millis): AccessState
}

sealed class AccessState
data class AccessGranted(val remainingDays: Int) : AccessState()
data class GracePeriod(val remainingDays: Int) : AccessState()
data object AccessDeniedLicenseOutdated : AccessState()
