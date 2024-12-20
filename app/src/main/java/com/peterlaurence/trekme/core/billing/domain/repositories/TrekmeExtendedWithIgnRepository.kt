package com.peterlaurence.trekme.core.billing.domain.repositories

import com.peterlaurence.trekme.core.billing.data.model.SubscriptionType
import com.peterlaurence.trekme.core.billing.di.IGN
import com.peterlaurence.trekme.core.billing.domain.api.BillingApi
import com.peterlaurence.trekme.core.billing.domain.model.ExtendedOfferStateOwner
import com.peterlaurence.trekme.core.billing.domain.model.PurchaseState
import com.peterlaurence.trekme.core.billing.domain.model.SubscriptionDetails
import com.peterlaurence.trekme.di.MainDispatcher
import com.peterlaurence.trekme.util.recoverLogged
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
    @IGN private val billingApi: BillingApi<SubscriptionType.MonthAndYear>,
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
            val ackDone = billingApi.queryAndAcknowledgePurchases()

            /* Otherwise, do normal checks */
            if (!ackDone) {
                updatePurchaseState()
            } else {
                onPurchaseAcknowledged()
            }
        }
    }

    suspend fun updatePurchaseState() {
        val result = if (billingApi.queryWhetherWeHavePurchasesAndConsumeOneTimePurchase()) {
            PurchaseState.PURCHASED
        } else {
            updateSubscriptionInfo()
            PurchaseState.NOT_PURCHASED
        }
        _purchaseFlow.value = result
    }

    fun acknowledgePurchase() = scope.launch {
        val ackDone = billingApi.queryAndAcknowledgePurchases()
        if (ackDone) {
            onPurchaseAcknowledged()
        }
    }

    private fun updateSubscriptionInfo() {
        scope.launch {
            recoverLogged {
                _yearlySubDetailsFlow.value =
                    billingApi.getSubscriptionDetails(SubscriptionType.MonthAndYear.Year)
            }
        }
        scope.launch {
            recoverLogged {
                _monthlySubDetailsFlow.value =
                    billingApi.getSubscriptionDetails(SubscriptionType.MonthAndYear.Month)
            }
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