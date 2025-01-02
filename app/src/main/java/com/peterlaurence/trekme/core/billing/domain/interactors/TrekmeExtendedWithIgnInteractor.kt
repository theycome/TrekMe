package com.peterlaurence.trekme.core.billing.domain.interactors

import com.peterlaurence.trekme.core.billing.data.model.SubscriptionType
import com.peterlaurence.trekme.core.billing.domain.repositories.TrekmeExtendedWithIgnRepository
import javax.inject.Inject

class TrekmeExtendedWithIgnInteractor @Inject constructor(
    private val repository: TrekmeExtendedWithIgnRepository,
) {
    fun buyMonthlySubscription() {
        repository.buySubscription(SubscriptionType.MonthAndYear.Month)
    }

    fun buyYearlySubscription() {
        repository.buySubscription(SubscriptionType.MonthAndYear.Year)
    }

    fun acknowledgePurchase() {
        repository.acknowledgePurchase()
    }
}
