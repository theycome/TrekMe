package com.peterlaurence.trekme.core.billing.data.api

/**
 * Created by Ivan Yakushev on 24.10.2024
 */
class PurchaseSKU(
    val oneTimeId: String,
    val subIdList: List<String>,
) {

    fun contains(sku: String) =
        (sku == oneTimeId || sku in subIdList)

}
