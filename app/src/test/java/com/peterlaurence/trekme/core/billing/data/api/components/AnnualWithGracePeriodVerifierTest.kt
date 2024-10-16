package com.peterlaurence.trekme.core.billing.data.api.components

import android.text.format.DateUtils
import com.peterlaurence.trekme.core.billing.domain.model.AccessGranted
import com.peterlaurence.trekme.util.datetime.Millis
import com.peterlaurence.trekme.util.datetime.days_
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

        verifier.checkTime(now + 1.millis, now) shouldBe
            AccessGranted(validityDuration)

        verifier.checkTime(now + 1.days_.millis, now) shouldBe
            AccessGranted(validityDuration + 1)

        verifier.checkTime(now + 115.days_.millis, now) shouldBe
            AccessGranted(validityDuration + 115)

        verifier.checkTime(
            now + (115.days_.millis + 1.days_.millis - 1.millis),
            now
        ) shouldBe
            AccessGranted(validityDuration + 115)

        verifier.checkTime(
            now + (115.days_.millis + 1.days_.millis),
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

    @Test
    fun `checkTime Granted`() {

//        verifier.checkTime(now + 1.millis, now) shouldBe
//            AccessGranted(validityDuration)

    }

}
