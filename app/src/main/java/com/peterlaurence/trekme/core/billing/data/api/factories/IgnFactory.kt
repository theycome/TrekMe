package com.peterlaurence.trekme.core.billing.data.api.factories

import android.app.Application
import com.peterlaurence.trekme.core.billing.data.api.Billing
import com.peterlaurence.trekme.core.billing.data.api.components.AnnualWithGracePeriodVerifier
import com.peterlaurence.trekme.core.billing.data.model.PurchaseIdsMonthYear
import com.peterlaurence.trekme.core.billing.data.model.SubscriptionType
import com.peterlaurence.trekme.core.billing.domain.api.BillingApi
import com.peterlaurence.trekme.events.AppEventBus

private const val IGN_ONETIME_SKU = "ign_license"
private const val IGN_SUBSCRIPTION_YEAR_SKU = "ign_license_sub"
private const val IGN_SUBSCRIPTION_MONTH_SKU = "ign_license_sub_monthly"

fun buildIgnBilling(
    app: Application,
    appEventBus: AppEventBus,
): BillingApi<SubscriptionType.MonthAndYear> {

    val ids = PurchaseIdsMonthYear(
        oneTimeId = IGN_ONETIME_SKU,
        subIdMonth = IGN_SUBSCRIPTION_MONTH_SKU,
        subIdYear = IGN_SUBSCRIPTION_YEAR_SKU,
    )

    return Billing(
        app,
        ids,
        ids,
        AnnualWithGracePeriodVerifier(),
        appEventBus
    )
}
