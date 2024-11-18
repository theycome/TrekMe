package com.peterlaurence.trekme.core.billing.domain.api

import arrow.core.raise.Raise
import com.peterlaurence.trekme.core.billing.domain.model.GetSubscriptionDetailsFailure
import com.peterlaurence.trekme.core.billing.domain.model.SubscriptionDetails
import kotlinx.coroutines.flow.Flow

interface BillingApi {

    val purchaseAcknowledgedEvent: Flow<Unit>

    suspend fun isPurchased(): Boolean

    suspend fun acknowledgePurchase(): Boolean

    context(Raise<GetSubscriptionDetailsFailure>)
    suspend fun getSubscriptionDetails(index: Int): SubscriptionDetails

    fun launchBilling(subscription: SubscriptionDetails, onPurchasePending: () -> Unit)

}
