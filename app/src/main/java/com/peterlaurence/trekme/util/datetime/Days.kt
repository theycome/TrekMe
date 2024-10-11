package com.peterlaurence.trekme.util.datetime

import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.DurationUnit

/**
 * Created by Ivan Yakushev on 06.09.2024
 */
@JvmInline
value class Days(val duration: Duration) {

    operator fun plus(other: Days): Days =
        Days(duration.plus(other.duration))

    operator fun minus(other: Days): Days =
        Days(duration.minus(other.duration))

    operator fun compareTo(other: Days): Int =
        duration.compareTo(other.duration)

    companion object {
        val allowableRange = Int.MIN_VALUE..Int.MAX_VALUE
        val allowableRangeNoOverflows = allowableRange.first / 2..allowableRange.last / 2
    }

}

/**
 * conversions from
 */
val Days.int: Int
    get() = duration.toInt(DurationUnit.DAYS)

val Days.long: Long
    get() = duration.toLong(DurationUnit.DAYS)

/**
 * conversions to
 */

/**
 * use `days_` with an `_` suffix to avoid ambiguity with likely named `Duration.days`
 */
val Int.days_: Days
    get() = Days(days)

val Long.days_: Days
    get() = Days(days)

val Duration.days_: Days
    get() = Days(this.toLong(DurationUnit.DAYS).days)
