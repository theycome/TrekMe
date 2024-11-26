package com.peterlaurence.trekme.util.datetime

import io.kotest.matchers.shouldBe
import kotlin.test.Test

/**
 * Created by Ivan Yakushev on 25.11.2024
 */
class DaysTest {

    @Test
    fun format() {
        val template = "%1\$dd of free trial"
        val days = 101.days_
        days.format(template) shouldBe "101d of free trial"
    }

}
