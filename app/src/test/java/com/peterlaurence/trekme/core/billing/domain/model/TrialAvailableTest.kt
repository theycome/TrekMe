package com.peterlaurence.trekme.core.billing.domain.model

import com.peterlaurence.trekme.util.datetime.days_
import com.peterlaurence.trekme.util.randomizers.IntRangeRandomizer
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/**
 * Created by Ivan Yakushev on 26.11.2024
 */

/**
 * Trial periods are given in the form of "P1W" -> 1 week, or "P4D" -> 4 days.
 */
class TrialAvailableTest {

    @Test
    fun `wrong parse format`() {

        TrialAvailable("100Q") shouldBe null
        TrialAvailable("-") shouldBe null
        TrialAvailable(" ") shouldBe null
        TrialAvailable("W") shouldBe null

    }

    @Test
    fun `empty period`() {
        TrialAvailable("") shouldBe TrialAvailable(0.days_)
    }

    @Test
    fun `successful parse`() {

        val randomizer = IntRangeRandomizer(10, 0..10_000)

        randomizer.forEach { (it, _) ->
            withClue("$it") {
                TrialAvailable("P${it}D") shouldBe TrialAvailable(it.days_)
            }
        }

        randomizer.forEach { (it, _) ->
            withClue("$it") {
                TrialAvailable("P${it}W") shouldBe TrialAvailable((it * 7).days_)
            }
        }
        
    }

}
