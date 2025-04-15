package com.peterlaurence.trekme.core.billing.domain.api

import arrow.core.Either
import com.peterlaurence.trekme.core.billing.data.model.SubscriptionDetails
import com.peterlaurence.trekme.core.billing.data.model.SubscriptionType
import com.peterlaurence.trekme.core.billing.domain.model.GetSubscriptionDetailsFailure
import kotlinx.coroutines.flow.Flow

interface BillingApi<in T : SubscriptionType> {

    val purchaseAcknowledgedEvent: Flow<Unit>

    suspend fun queryWhetherWeHavePurchasesAndConsumeOneTimePurchase(): Boolean

    suspend fun queryAndAcknowledgePurchases(): Boolean

    suspend fun getSubscriptionDetails(subscriptionType: T): Either<GetSubscriptionDetailsFailure, SubscriptionDetails>

    fun launchBilling(subscription: SubscriptionDetails, onPurchasePending: () -> Unit)

}
