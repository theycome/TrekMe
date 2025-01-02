package com.peterlaurence.trekme.core.billing.domain.repositories

import com.peterlaurence.trekme.core.billing.data.model.SubscriptionDetails
import com.peterlaurence.trekme.core.billing.data.model.SubscriptionType
import com.peterlaurence.trekme.core.billing.domain.api.BillingApi
import com.peterlaurence.trekme.core.billing.domain.model.PurchaseState

/**
 * Created by Ivan Yakushev on 31.12.2024
 *
 * Collect the common functionality used by repository classes
 */
class PurchaseProcessor<T : SubscriptionType>(
    private val billing: BillingApi<T>,
    private val onNotPurchased: () -> Unit,
    private val onUpdatePurchaseState: (PurchaseState) -> Unit,
) {

    suspend fun process() {
        if (!acknowledgePurchase()) {
            updatePurchaseState()
        }
    }

    suspend fun acknowledgePurchase(): Boolean =
        if (billing.queryAndAcknowledgePurchases()) {
            onPurchaseAcknowledged()
            true
        } else {
            false
        }

    suspend fun updatePurchaseState() {
        val state = if (billing.queryWhetherWeHavePurchasesAndConsumeOneTimePurchase()) {
            PurchaseState.PURCHASED
        } else {
            onNotPurchased()
            PurchaseState.NOT_PURCHASED
        }
        onUpdatePurchaseState(state)
    }

    fun launchBillingWith(subscriptionDetails: SubscriptionDetails) {
        billing.launchBilling(subscriptionDetails) {
            onPurchasePending()
        }
    }

    fun onPurchaseAcknowledged() {
        onUpdatePurchaseState(PurchaseState.PURCHASED)
    }

    private fun onPurchasePending() {
        onUpdatePurchaseState(PurchaseState.PURCHASE_PENDING)
    }

}
