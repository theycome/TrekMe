package com.peterlaurence.trekme.util.datetime

import android.text.format.DateUtils
import arrow.core.raise.recover
import com.peterlaurence.trekme.util.ext.OperationOnLongFailure
import com.peterlaurence.trekme.util.ext.minusSafe
import com.peterlaurence.trekme.util.ext.plusSafe
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Date
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

/**
 * Created by Ivan Yakushev on 06.09.2024
 * @param long could be negative
 */
@JvmInline
value class Millis(val long: Long) {

    operator fun plus(other: Millis?): Millis {
        val rhs = other ?: return this

        val a = long
        val b = rhs.long

        return recover({ a.plusSafe(b, allowableRange) }) {
            when (it) {
                OperationOnLongFailure.OVERFLOW -> error("Overflow when performing $a plus $b")
                OperationOnLongFailure.UNDERFLOW -> error("Underflow when performing $a plus $b")
            }
        }.millis
    }

    operator fun minus(other: Millis?): Millis {
        val rhs = other ?: return this

        val a = long
        val b = rhs.long

        return recover({ a.minusSafe(b, allowableRange) }) {
            when (it) {
                OperationOnLongFailure.OVERFLOW -> error("Overflow when performing $a minus $b")
                OperationOnLongFailure.UNDERFLOW -> error("Underflow when performing $a minus $b")
            }
        }.millis
    }

    operator fun compareTo(other: Millis): Int = long.compareTo(other.long)

    fun abs(): Millis = abs(long).millis

    companion object {

        val allowableRange = Long.MIN_VALUE..Long.MAX_VALUE

        val allowableRangeNoOverflows = allowableRange.first / 2..allowableRange.last / 2

        fun nowDefaultTimeZone(): Millis =
            LocalDateTime.now().withNano(0).millis

        fun nowUTC(): Millis = Date().time.millis

    }

}

/**
 * conversions from
 */
val Millis.days_: Days
    get() = this.long.milliseconds.inWholeDays.days_

/**
 * conversions to
 */
val Long.millis: Millis
    get() = Millis(this)

val Int.millis: Millis
    get() = toLong().millis

val LocalDate.millis: Millis
    get() = (toEpochDay() * DateUtils.DAY_IN_MILLIS).millis

val LocalTime.millis: Millis
    get() = (hour * DateUtils.HOUR_IN_MILLIS +
        minute * DateUtils.MINUTE_IN_MILLIS +
        second * DateUtils.SECOND_IN_MILLIS).millis

val LocalDateTime.millis: Millis
    get() = toLocalDate().millis + toLocalTime().millis
