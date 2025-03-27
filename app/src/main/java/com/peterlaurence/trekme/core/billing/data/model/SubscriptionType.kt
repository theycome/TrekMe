package com.peterlaurence.trekme.core.billing.data.model

/**
 * Created by Ivan Yakushev on 19.11.2024
 */
sealed interface SubscriptionType {

    // TODO - rename as OneTime
    data object Single : SubscriptionType

    // TODO - rename as Timed
    sealed interface MonthAndYear : SubscriptionType {
        data object Month : MonthAndYear
        data object Year : MonthAndYear
    }

}
