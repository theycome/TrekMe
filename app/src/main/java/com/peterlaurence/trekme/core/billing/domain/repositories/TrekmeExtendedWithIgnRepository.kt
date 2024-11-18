package com.peterlaurence.trekme.core.billing.domain.repositories

import arrow.core.raise.recover
import com.peterlaurence.trekme.core.billing.di.IGN
import com.peterlaurence.trekme.core.billing.domain.api.BillingApi
import com.peterlaurence.trekme.core.billing.domain.model.ExtendedOfferStateOwner
import com.peterlaurence.trekme.core.billing.domain.model.PurchaseState
import com.peterlaurence.trekme.core.billing.domain.model.SubscriptionDetails
import com.peterlaurence.trekme.di.MainDispatcher
import com.peterlaurence.trekme.util.log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrekmeExtendedWithIgnRepository @Inject constructor(
    @MainDispatcher mainDispatcher: CoroutineDispatcher,
    @IGN private val billingApi: BillingApi,
) : ExtendedOfferStateOwner {
    private val scope = CoroutineScope(mainDispatcher + SupervisorJob())

    private val _purchaseFlow = MutableStateFlow(PurchaseState.CHECK_PENDING)
    override val purchaseFlow = _purchaseFlow.asStateFlow()

    private val _yearlySubDetailsFlow = MutableStateFlow<SubscriptionDetails?>(null)
    override val yearlySubDetailsFlow = _yearlySubDetailsFlow.asStateFlow()

    private val _monthlySubDetailsFlow = MutableStateFlow<SubscriptionDetails?>(null)
    override val monthlySubDetailsFlow = _monthlySubDetailsFlow.asStateFlow()

    init {
        scope.launch {
            billingApi.purchaseAcknowledgedEvent.collect {
                onPurchaseAcknowledged()
            }
        }

        scope.launch {

            /* Check if we just need to acknowledge the purchase */
            val ackDone = billingApi.acknowledgePurchase()

            /* Otherwise, do normal checks */
            if (!ackDone) {
                updatePurchaseState()
            } else {
                onPurchaseAcknowledged()
            }
        }
    }

    suspend fun updatePurchaseState() {
        val result = if (billingApi.isPurchased()) {
            PurchaseState.PURCHASED
        } else {
            updateSubscriptionInfo()
            PurchaseState.NOT_PURCHASED
        }
        _purchaseFlow.value = result
    }

    fun acknowledgePurchase() = scope.launch {
        val ackDone = billingApi.acknowledgePurchase()
        if (ackDone) {
            onPurchaseAcknowledged()
        }
    }

    private fun updateSubscriptionInfo() {
        scope.launch {
            recover({
                _yearlySubDetailsFlow.value = billingApi.getSubscriptionDetails(1)
            }) { this@TrekmeExtendedWithIgnRepository.log(it) }
        }
        scope.launch {
            recover({
                _monthlySubDetailsFlow.value = billingApi.getSubscriptionDetails(0)
            }) { this@TrekmeExtendedWithIgnRepository.log(it) }
        }
    }

    fun buyYearlySubscription() {
        val subscriptionDetails = _yearlySubDetailsFlow.value
        if (subscriptionDetails != null) {
            billingApi.launchBilling(subscriptionDetails, ::onPurchasePending)
        }
    }

    fun buyMonthlySubscription() {
        val subscriptionDetails = _monthlySubDetailsFlow.value
        if (subscriptionDetails != null) {
            billingApi.launchBilling(subscriptionDetails, ::onPurchasePending)
        }
    }

    private fun onPurchasePending() {
        _purchaseFlow.value = PurchaseState.PURCHASE_PENDING
    }

    private fun onPurchaseAcknowledged() {
        _purchaseFlow.value = PurchaseState.PURCHASED
    }
}