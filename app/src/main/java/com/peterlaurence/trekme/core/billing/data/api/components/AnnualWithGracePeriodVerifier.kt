package com.peterlaurence.trekme.core.billing.data.api.components

import com.peterlaurence.trekme.core.billing.domain.model.AccessDeniedLicenseOutdated
import com.peterlaurence.trekme.core.billing.domain.model.AccessGranted
import com.peterlaurence.trekme.core.billing.domain.model.AccessState
import com.peterlaurence.trekme.core.billing.domain.model.GracePeriod
import com.peterlaurence.trekme.core.billing.domain.model.PurchaseVerifier
import com.peterlaurence.trekme.util.datetime.Days
import com.peterlaurence.trekme.util.datetime.Millis
import com.peterlaurence.trekme.util.datetime.days_
import com.peterlaurence.trekme.util.datetime.int
import java.util.Date

/**
 * number of days the user is allowed to use the app despite expired license
 */
private val GRACE_PERIOD: Days = 15.days_

private val VALIDITY_DURATION: Days = 365.days_ - GRACE_PERIOD

class AnnualWithGracePeriodVerifier : PurchaseVerifier {

    /**
     * The billing API uses a purchase time in milliseconds since the epoch (Jan 1, 1970), which is
     * exactly the same as what we get with [Date.getTime].
     * So we obtain the current time in millis and convert the difference with the purchase time in
     * days.
     */
    override fun checkTime(purchaseTime: Millis, now: Millis): AccessState {

        val sincePurchase = now - purchaseTime
        val daysToPurchase = sincePurchase.abs().days_

        return if (sincePurchase.value > 0) {
            when {
                (daysToPurchase <= VALIDITY_DURATION) ->
                    AccessGranted((VALIDITY_DURATION - daysToPurchase).int)

                (daysToPurchase < (VALIDITY_DURATION + GRACE_PERIOD)) ->
                    GracePeriod((VALIDITY_DURATION + GRACE_PERIOD - daysToPurchase).int)

                else -> AccessDeniedLicenseOutdated
            }
        } else {
            AccessGranted((VALIDITY_DURATION + daysToPurchase).int) // purchase happened "in the future"
        }
    }

}

