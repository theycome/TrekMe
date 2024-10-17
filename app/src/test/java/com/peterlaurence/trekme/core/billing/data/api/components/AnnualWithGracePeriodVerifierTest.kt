package com.peterlaurence.trekme.core.billing.data.api.components

import android.text.format.DateUtils
import com.peterlaurence.trekme.core.billing.domain.model.AccessGranted
import com.peterlaurence.trekme.util.datetime.Millis
import com.peterlaurence.trekme.util.datetime.days_
import com.peterlaurence.trekme.util.datetime.int
import com.peterlaurence.trekme.util.datetime.millis
import com.peterlaurence.trekme.util.randomizers.IntRangeRandomizer
import com.peterlaurence.trekme.util.randomizers.LongRangeRandomizer
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/**
 * Created by Ivan Yakushev on 13.10.2024
 */
class AnnualWithGracePeriodVerifierTest {

    private val gracePeriod = 15.days_
    private val validityDuration = 350.days_
    private val verifier = AnnualWithGracePeriodVerifier()
    private val now = Millis.now()

    @Test
    fun `checkTime future`() {

        verifier.checkTime(now + 1.millis, now) shouldBe
            AccessGranted(validityDuration.int)

        verifier.checkTime(now + 1.days_.millis, now) shouldBe
            AccessGranted((validityDuration + 1.days_).int)

        verifier.checkTime(now + 115.days_.millis, now) shouldBe
            AccessGranted((validityDuration + 115.days_).int)

        verifier.checkTime(
            now + (115.days_.millis + 1.days_.millis - 1.millis),
            now
        ) shouldBe
            AccessGranted((validityDuration + 115.days_).int)

        verifier.checkTime(
            now + (115.days_.millis + 1.days_.millis),
            now
        ) shouldBe
            AccessGranted((validityDuration + 115.days_ + 1.days_).int)

    }

    @Test
    fun `checkTime future random`() {

        val randomizer = IntRangeRandomizer(7, 0..Int.MAX_VALUE)

        randomizer.forEach { (it, _) ->
            withClue("$it") {

                val days = it / DateUtils.DAY_IN_MILLIS.toInt()

                verifier.checkTime(
                    now + (it).millis,
                    now
                ) shouldBe
                    AccessGranted((validityDuration + days.days_).int)

            }
        }

    }

    @Test
    fun `checkTime Granted`() {

        verifier.checkTime(now - 1.millis, now) shouldBe
            AccessGranted(validityDuration.int)

        verifier.checkTime(now - 1.days_.millis, now) shouldBe
            AccessGranted((validityDuration - 1.days_).int)

        verifier.checkTime(now - 115.days_.millis, now) shouldBe
            AccessGranted((validityDuration - 115.days_).int)

        verifier.checkTime(
            now - (115.days_.millis + 1.days_.millis + 1.millis),
            now
        ) shouldBe
            AccessGranted((validityDuration - 115.days_ - 1.days_).int)

        verifier.checkTime(
            now - (115.days_.millis + 1.days_.millis),
            now
        ) shouldBe
            AccessGranted((validityDuration - 115.days_ - 1.days_).int)

    }

    @Test
    fun `checkTime Granted random`() {

        val randomizer = LongRangeRandomizer(7, 0..validityDuration.millis.long)

        randomizer.forEach { (it, _) ->
            withClue("$it") {

                val millis = it.millis

                verifier.checkTime(
                    now - millis,
                    now
                ) shouldBe
                    AccessGranted((validityDuration - millis.days_).int)

            }
        }

    }

}
