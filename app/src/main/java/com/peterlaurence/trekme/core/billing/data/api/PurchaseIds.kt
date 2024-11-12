package com.peterlaurence.trekme.core.billing.data.api

/**
 * Created by Ivan Yakushev on 24.10.2024
 */
data class PurchaseIds(
    val oneTimeId: String,
    val subIdList: List<String>,
) {

    operator fun contains(id: String) =
        (containsOneTime(id) || containsSub(id))

    fun containsOneTime(id: String) =
        (id == oneTimeId)

    fun containsSub(id: String) =
        (id in subIdList)

}
