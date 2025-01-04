package com.peterlaurence.trekme.core.billing.domain.repositories.interactors

import com.peterlaurence.trekme.core.billing.data.model.SubscriptionDetails
import com.peterlaurence.trekme.core.billing.data.model.SubscriptionType
import com.peterlaurence.trekme.core.billing.domain.api.BillingApi
import com.peterlaurence.trekme.core.billing.domain.model.PurchaseState
import com.peterlaurence.trekme.util.recoverLogged
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Created by Ivan Yakushev on 04.01.2025
 */
interface PurchaseInteractor<T : SubscriptionType> {

    val billing: BillingApi<T>
    val scope: CoroutineScope
    val onUpdatePurchaseState: (PurchaseState) -> Unit
    val subscriptionInteractor: SubscriptionInteractor<T>

    // TODO - maybe try to further divide class, draw Sequence diagram

    operator fun invoke() {
        passDownPurchaseAcknowledgedEvent()
        processPurchase()
    }

    fun acknowledgePurchase() {
        scope.launch {
            acknowledgePurchaseSuspended()
        }
    }

    fun buySubscription(subscriptionType: T) {
        subscriptionInteractor(subscriptionType) { interactor ->
            interactor.producer()?.let(::launchBillingWith)
        }
    }

    suspend fun updatePurchaseState() {
        val state = if (billing.queryWhetherWeHavePurchasesAndConsumeOneTimePurchase()) {
            PurchaseState.PURCHASED
        } else {
            querySubscriptions()
            PurchaseState.NOT_PURCHASED
        }
        onUpdatePurchaseState(state)
    }

    private fun passDownPurchaseAcknowledgedEvent() {
        scope.launch {
            billing.purchaseAcknowledgedEvent.collect {
                onPurchaseAcknowledged()
            }
        }
    }

    private fun processPurchase() {
        scope.launch {
            if (!acknowledgePurchaseSuspended()) {
                updatePurchaseState()
            }
        }
    }

    private fun launchBillingWith(subscriptionDetails: SubscriptionDetails) {
        billing.launchBilling(subscriptionDetails) {
            onPurchasePending()
        }
    }

    private suspend fun acknowledgePurchaseSuspended(): Boolean =
        if (billing.queryAndAcknowledgePurchases()) {
            onPurchaseAcknowledged()
            true
        } else {
            false
        }

    private fun querySubscriptions() {
        subscriptionInteractor.types.forEach { type ->
            subscriptionInteractor(type) { interactor ->
                scope.launch {
                    recoverLogged {

                        val subscriptionDetails = billing.getSubscriptionDetails(type)
                        interactor.consumer(subscriptionDetails)

                    }
                }
            }
        }
    }

    private fun onPurchaseAcknowledged() {
        onUpdatePurchaseState(PurchaseState.PURCHASED)
    }

    private fun onPurchasePending() {
        onUpdatePurchaseState(PurchaseState.PURCHASE_PENDING)
    }

}