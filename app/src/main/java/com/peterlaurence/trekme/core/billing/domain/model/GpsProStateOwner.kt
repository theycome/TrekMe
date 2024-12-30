package com.peterlaurence.trekme.core.billing.domain.model

import com.peterlaurence.trekme.core.billing.data.model.SubscriptionDetails
import kotlinx.coroutines.flow.StateFlow

interface GpsProStateOwner {
    val purchaseFlow: StateFlow<PurchaseState>
    val subDetailsFlow: StateFlow<SubscriptionDetails?>
}