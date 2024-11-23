package com.peterlaurence.trekme.core.billing.data.api.factories

import android.app.Application
import com.peterlaurence.trekme.core.billing.data.api.Billing
import com.peterlaurence.trekme.core.billing.data.api.components.AnnualWithGracePeriodVerifier
import com.peterlaurence.trekme.core.billing.data.model.PurchaseIdsSingle
import com.peterlaurence.trekme.core.billing.data.model.SubscriptionType
import com.peterlaurence.trekme.core.billing.domain.api.BillingApi
import com.peterlaurence.trekme.events.AppEventBus

private const val ONETIME_SKU = "gps_pro"
private const val SUBSCRIPTION_SKU = "gps_pro_sub"

fun buildGpsProBilling(
    app: Application,
    appEventBus: AppEventBus,
): BillingApi<SubscriptionType.Single> {

    val ids = PurchaseIdsSingle(
        oneTimeId = ONETIME_SKU,
        subId = SUBSCRIPTION_SKU,
    )

    return Billing(
        app,
        ids,
        ids,
        AnnualWithGracePeriodVerifier(),
        appEventBus,
    )
}
