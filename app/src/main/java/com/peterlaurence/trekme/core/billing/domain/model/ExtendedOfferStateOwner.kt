package com.peterlaurence.trekme.core.billing.domain.model

import com.peterlaurence.trekme.core.billing.data.model.SubscriptionDetails
import kotlinx.coroutines.flow.StateFlow

interface ExtendedOfferStateOwner {
    val purchaseFlow: StateFlow<PurchaseState>
    val yearlySubDetailsFlow: StateFlow<SubscriptionDetails?>
    val monthlySubDetailsFlow: StateFlow<SubscriptionDetails?>
}