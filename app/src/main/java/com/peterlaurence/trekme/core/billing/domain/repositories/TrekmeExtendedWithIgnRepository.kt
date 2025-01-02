package com.peterlaurence.trekme.core.billing.domain.repositories

import com.peterlaurence.trekme.core.billing.data.model.SubscriptionDetails
import com.peterlaurence.trekme.core.billing.data.model.SubscriptionType
import com.peterlaurence.trekme.core.billing.di.IGN
import com.peterlaurence.trekme.core.billing.domain.api.BillingApi
import com.peterlaurence.trekme.core.billing.domain.model.ExtendedOfferStateOwner
import com.peterlaurence.trekme.core.billing.domain.model.PurchaseState
import com.peterlaurence.trekme.di.MainDispatcher
import com.peterlaurence.trekme.util.recoverLogged
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrekmeExtendedWithIgnRepository @Inject constructor(
    @MainDispatcher mainDispatcher: CoroutineDispatcher,
    @IGN private val billing: BillingApi<SubscriptionType.MonthAndYear>,
) : ExtendedOfferStateOwner {

    private val scope = CoroutineScope(mainDispatcher + SupervisorJob())

    private val _purchaseFlow = MutableStateFlow(PurchaseState.CHECK_PENDING)
    override val purchaseFlow = _purchaseFlow.asStateFlow()

    private val _yearlySubDetailsFlow = MutableStateFlow<SubscriptionDetails?>(null)
    override val yearlySubDetailsFlow = _yearlySubDetailsFlow.asStateFlow()

    private val _monthlySubDetailsFlow = MutableStateFlow<SubscriptionDetails?>(null)
    override val monthlySubDetailsFlow = _monthlySubDetailsFlow.asStateFlow()

    private val purchaseProcessor =
        PurchaseProcessor(
            billing = billing,
            onNotPurchased = ::updateSubscriptionInfo,
            onUpdatePurchaseState = { state ->
                _purchaseFlow.value = state
            }
        )

    init {
        scope.launch {
            billing.purchaseAcknowledgedEvent.collect {
                purchaseProcessor.onPurchaseAcknowledged()
            }
        }

        scope.launch {
            purchaseProcessor.process()
        }
    }

    suspend fun updatePurchaseState() {
        purchaseProcessor.updatePurchaseState()
    }

    fun acknowledgePurchase(): Job = scope.launch {
        purchaseProcessor.acknowledgePurchase()
    }

    private fun updateSubscriptionInfo() {
        scope.launch {
            recoverLogged {
                _yearlySubDetailsFlow.value =
                    billing.getSubscriptionDetails(SubscriptionType.MonthAndYear.Year)
            }
        }
        scope.launch {
            recoverLogged {
                _monthlySubDetailsFlow.value =
                    billing.getSubscriptionDetails(SubscriptionType.MonthAndYear.Month)
            }
        }
    }

    fun buySubscription(subscriptionType: SubscriptionType.MonthAndYear) {
        when (subscriptionType) {
            is SubscriptionType.MonthAndYear.Month -> monthlySubDetailsFlow.value
            is SubscriptionType.MonthAndYear.Year -> yearlySubDetailsFlow.value
        }?.let(purchaseProcessor::launchBillingWith)
    }

}
