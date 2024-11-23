package com.peterlaurence.trekme.core.billing.data.model

/**
 * Created by Ivan Yakushev on 24.10.2024
 */
interface PurchaseIdsContract {

    val oneTimeId: String
    val subIdList: List<String>

    operator fun contains(id: String) =
        (containsOneTime(id) || containsSub(id))

    fun containsOneTime(id: String) =
        (id == oneTimeId)

    fun containsSub(id: String) =
        (id in subIdList)

}

interface PurchaseIdsResolver<in T : SubscriptionType> {
    operator fun invoke(type: T): String
}

data class PurchaseIdsSingle(
    override val oneTimeId: String,
    private val subId: String,
) : PurchaseIdsContract, PurchaseIdsResolver<SubscriptionType.Single> {

    override val subIdList: List<String> =
        listOf(subId) // TODO - what if set in constructor - will this statement still override correctly

    override operator fun invoke(type: SubscriptionType.Single): String = subId

}

data class PurchaseIdsMonthYear(
    override val oneTimeId: String,
    private val subIdMonth: String,
    private val subIdYear: String,
) : PurchaseIdsContract, PurchaseIdsResolver<SubscriptionType.MonthAndYear> {

    override val subIdList: List<String> = listOf(subIdMonth, subIdYear)

    override operator fun invoke(type: SubscriptionType.MonthAndYear): String =
        when (type) {
            is SubscriptionType.MonthAndYear.Month -> subIdMonth
            is SubscriptionType.MonthAndYear.Year -> subIdYear
        }

}
