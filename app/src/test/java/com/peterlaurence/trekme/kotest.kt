package com.peterlaurence.trekme

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Created by Ivan Yakushev on 20.12.2024
 */
fun shouldNotHappen() {
    withClue("Code execution must not reach this line") {
        true shouldBe false
    }
}
