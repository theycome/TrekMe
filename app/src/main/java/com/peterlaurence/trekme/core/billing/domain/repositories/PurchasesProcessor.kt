package com.peterlaurence.trekme.core.billing.domain.repositories

import com.peterlaurence.trekme.core.billing.data.model.SubscriptionType
import com.peterlaurence.trekme.core.billing.domain.api.BillingApi
import com.peterlaurence.trekme.core.billing.domain.model.PurchaseState

/**
 * Created by Ivan Yakushev on 31.12.2024
 *
 * Collect the common functionality used by repository classes
 */
class PurchasesProcessor<T : SubscriptionType>(
    private val billing: BillingApi<T>,
    private val onPurchaseAcknowledged: () -> Unit,
    private val onNotPurchased: () -> Unit,
    private val onUpdatePurchaseState: (PurchaseState) -> Unit,
) {

    suspend operator fun invoke() {
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

}
