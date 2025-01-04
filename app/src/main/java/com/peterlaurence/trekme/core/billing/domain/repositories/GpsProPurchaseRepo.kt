package com.peterlaurence.trekme.core.billing.domain.repositories

import com.peterlaurence.trekme.core.billing.data.model.SubscriptionDetails
import com.peterlaurence.trekme.core.billing.data.model.SubscriptionType
import com.peterlaurence.trekme.core.billing.di.GpsPro
import com.peterlaurence.trekme.core.billing.domain.api.BillingApi
import com.peterlaurence.trekme.core.billing.domain.model.GpsProStateOwner
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
class GpsProPurchaseRepo @Inject constructor(
    @MainDispatcher mainDispatcher: CoroutineDispatcher,
    @GpsPro override val billing: BillingApi<SubscriptionType.Single>,
) : GpsProStateOwner,
    PurchaseInteractor<SubscriptionType.Single> {

    private val _purchaseFlow = MutableStateFlow(PurchaseState.CHECK_PENDING)
    override val purchaseFlow = _purchaseFlow.asStateFlow()

    private val _subDetailsFlow = MutableStateFlow<SubscriptionDetails?>(null)
    override val subDetailsFlow = _subDetailsFlow.asStateFlow()

    override val scope = CoroutineScope(mainDispatcher + SupervisorJob())

    override val onUpdatePurchaseState: (PurchaseState) -> Unit = {
        _purchaseFlow.value = it
    }

    override val subscriptionInteractor =
        SubscriptionInteractor(
            types = sealedSubclassesInstances<SubscriptionType.Single>(),
            providersMap = mapOf(
                SubscriptionType.Single to SubscriptionInteractor.SubscriptionDetailsProvider(
                    producer = { subDetailsFlow.value },
                    consumer = { _subDetailsFlow.value = it },
                )
            ),
        )

    init {
        super.invoke()
    }

}
