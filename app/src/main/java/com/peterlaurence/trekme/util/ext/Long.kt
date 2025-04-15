package com.peterlaurence.trekme.util.ext

import arrow.core.Either
import arrow.core.raise.either

/**
 * Created by Ivan Yakushev on 29.09.2024
 */
@Suppress("ComplexCondition", "Ensure")
fun Long.plusSafe(other: Long, allowableRange: LongRange): Either<OperationOnLongFailure, Long> =
    either {
        val res = this@plusSafe.plus(other)

        if (this@plusSafe > 0 && other > 0 && (res < 0 || res !in allowableRange)) {
            raise(OperationOnLongFailure.OVERFLOW)
        } else if (this@plusSafe < 0 && other < 0 && (res > 0 || res !in allowableRange)) {
            raise(OperationOnLongFailure.UNDERFLOW)
        }

        res
    }

@Suppress("ComplexCondition", "Ensure")
fun Long.minusSafe(other: Long, allowableRange: LongRange): Either<OperationOnLongFailure, Long> =
    either {
        val res = this@minusSafe.minus(other)

        if (this@minusSafe > 0 && other < 0 && (res < 0 || res !in allowableRange)) {
            raise(OperationOnLongFailure.OVERFLOW)
        } else if (this@minusSafe < 0 && other > 0 && (res > 0 || res !in allowableRange)) {
            raise(OperationOnLongFailure.UNDERFLOW)
        }

        res
    }

enum class OperationOnLongFailure { OVERFLOW, UNDERFLOW }
