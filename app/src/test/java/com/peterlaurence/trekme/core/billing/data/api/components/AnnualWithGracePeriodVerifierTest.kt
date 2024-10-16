package com.peterlaurence.trekme.core.billing.data.api.components

import android.text.format.DateUtils
import com.peterlaurence.trekme.core.billing.domain.model.AccessGranted
import com.peterlaurence.trekme.util.datetime.Millis
import com.peterlaurence.trekme.util.datetime.millis
import com.peterlaurence.trekme.util.randomizers.IntRangeRandomizer
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/**
 * Created by Ivan Yakushev on 13.10.2024
 */
class AnnualWithGracePeriodVerifierTest {

    private val gracePeriod = 15
    private val validityDuration = 350
    private val verifier = AnnualWithGracePeriodVerifier()
    private val now = Millis.now()

    @Test
    fun `checkTime future`() {

        // +1ms
        verifier.checkTime(now + 1.millis, now) shouldBe
            AccessGranted(validityDuration)

        // +1d
        verifier.checkTime(now + DateUtils.DAY_IN_MILLIS.millis, now) shouldBe
            AccessGranted(validityDuration + 1)

        // +115d
        verifier.checkTime(now + (DateUtils.DAY_IN_MILLIS * 115).millis, now) shouldBe
            AccessGranted(validityDuration + 115)

        // +115d + 1d - 1ms
        verifier.checkTime(
            now + (DateUtils.DAY_IN_MILLIS * 115 + DateUtils.DAY_IN_MILLIS - 1).millis,
            now
        ) shouldBe
            AccessGranted(validityDuration + 115)

        // +115d + 1d
        verifier.checkTime(
            now + (DateUtils.DAY_IN_MILLIS * 115 + DateUtils.DAY_IN_MILLIS).millis,
            now
        ) shouldBe
            AccessGranted(validityDuration + 115 + 1)

    }

    @Test
    fun `checkTime future random`() {

        val randomizer = IntRangeRandomizer(7, 0..Int.MAX_VALUE)

        randomizer.forEach { (a, _) ->
            withClue("$a") {
                val daysInMillis = a / DateUtils.DAY_IN_MILLIS.toInt()

                verifier.checkTime(
                    now + (a).millis,
                    now
                ) shouldBe
                    AccessGranted(validityDuration + daysInMillis)

            }
        }

    }

}
