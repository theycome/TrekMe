package com.peterlaurence.trekme.core.billing.domain.api

import arrow.core.raise.Raise
import com.peterlaurence.trekme.core.billing.data.model.SubscriptionType
import com.peterlaurence.trekme.core.billing.domain.model.GetSubscriptionDetailsFailure
import com.peterlaurence.trekme.core.billing.domain.model.SubscriptionDetails
import kotlinx.coroutines.flow.Flow

interface BillingApi<in T : SubscriptionType> {

    val purchaseAcknowledgedEvent: Flow<Unit>

    suspend fun isPurchased(): Boolean

    suspend fun acknowledgePurchase(): Boolean

    context(Raise<GetSubscriptionDetailsFailure>)
    suspend fun getSubscriptionDetails(subscriptionType: T): SubscriptionDetails

    fun launchBilling(subscription: SubscriptionDetails, onPurchasePending: () -> Unit)

}
