package com.peterlaurence.trekme.util.ext

import arrow.core.raise.Raise

/**
 * Created by Ivan Yakushev on 29.09.2024
 */
context(Raise<OperationOnLongFailure>)
@Suppress("ComplexCondition")
fun Long.plusSafe(other: Long, allowableRange: LongRange): Long {
    val res = this.plus(other)

    if (this > 0 && other > 0 && (res < 0 || res !in allowableRange)) {
        raise(OperationOnLongFailure.OVERFLOW)
    } else if (this < 0 && other < 0 && (res > 0 || res !in allowableRange)) {
        raise(OperationOnLongFailure.UNDERFLOW)
    }

    return res
}

context(Raise<OperationOnLongFailure>)
@Suppress("ComplexCondition")
fun Long.minusSafe(other: Long, allowableRange: LongRange): Long {
    val res = this.minus(other)

    if (this > 0 && other < 0 && (res < 0 || res !in allowableRange)) {
        raise(OperationOnLongFailure.OVERFLOW)
    } else if (this < 0 && other > 0 && (res > 0 || res !in allowableRange)) {
        raise(OperationOnLongFailure.UNDERFLOW)
    }

    return res
}

enum class OperationOnLongFailure { OVERFLOW, UNDERFLOW }
