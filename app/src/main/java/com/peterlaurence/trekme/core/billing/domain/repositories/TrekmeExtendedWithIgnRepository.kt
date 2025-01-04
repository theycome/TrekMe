package com.peterlaurence.trekme.core.billing.domain.repositories

import com.peterlaurence.trekme.core.billing.data.model.SubscriptionDetails
import com.peterlaurence.trekme.core.billing.data.model.SubscriptionType
import com.peterlaurence.trekme.core.billing.di.IGN
import com.peterlaurence.trekme.core.billing.domain.api.BillingApi
import com.peterlaurence.trekme.core.billing.domain.model.ExtendedOfferStateOwner
import com.peterlaurence.trekme.core.billing.domain.model.PurchaseState
import com.peterlaurence.trekme.core.billing.domain.repositories.interactors.PurchaseInteractor
import com.peterlaurence.trekme.core.billing.domain.repositories.interactors.SubscriptionInteractor
import com.peterlaurence.trekme.di.MainDispatcher
import com.peterlaurence.trekme.util.sealedSubclassesInstances
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrekmeExtendedWithIgnRepository @Inject constructor(
    @MainDispatcher mainDispatcher: CoroutineDispatcher,
    @IGN override val billing: BillingApi<SubscriptionType.MonthAndYear>,
) : ExtendedOfferStateOwner,
    PurchaseInteractor<SubscriptionType.MonthAndYear> {

    private val _purchaseFlow = MutableStateFlow(PurchaseState.CHECK_PENDING)
    override val purchaseFlow = _purchaseFlow.asStateFlow()

    private val _monthlySubDetailsFlow = MutableStateFlow<SubscriptionDetails?>(null)
    override val monthlySubDetailsFlow = _monthlySubDetailsFlow.asStateFlow()

    private val _yearlySubDetailsFlow = MutableStateFlow<SubscriptionDetails?>(null)
    override val yearlySubDetailsFlow = _yearlySubDetailsFlow.asStateFlow()

    override val scope = CoroutineScope(mainDispatcher + SupervisorJob())

    override val onUpdatePurchaseState: (PurchaseState) -> Unit = {
        _purchaseFlow.value = it
    }

    override val subscriptionInteractor: SubscriptionInteractor<SubscriptionType.MonthAndYear> =
        SubscriptionInteractor(
            types = sealedSubclassesInstances<SubscriptionType.MonthAndYear>(),
            providersMap = mapOf(
                SubscriptionType.MonthAndYear.Month to SubscriptionInteractor.SubscriptionDetailsProvider(
                    producer = { monthlySubDetailsFlow.value },
                    consumer = { _monthlySubDetailsFlow.value = it },
                ),
                SubscriptionType.MonthAndYear.Year to SubscriptionInteractor.SubscriptionDetailsProvider(
                    producer = { yearlySubDetailsFlow.value },
                    consumer = { _yearlySubDetailsFlow.value = it },
                )
            ),
        )

    init {
        super.invoke()
    }

}
