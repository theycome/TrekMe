package com.peterlaurence.trekme.util.randomizers

import kotlin.random.Random

/**
 * Created by Ivan Yakushev on 16.10.2024
 */
open class RangeRandomizer<T : Comparable<T>, R : ClosedRange<T>>(
    private val size: Int,
    range: R,
    includesRangeBorders: Boolean,
    generate: (T, T) -> T,
) : Iterator<Pair<T, T>> {

    private var index = 0

    private val values = Array(size) {
        generate(range.start, range.endInclusive) to generate(range.start, range.endInclusive)
    }.apply {
        if (includesRangeBorders) {
            this[0] = range.start to range.endInclusive
        }
    }

    override fun hasNext() = index < size

    override fun next(): Pair<T, T> {
        if (!this.hasNext()) {
            throw NoSuchElementException()
        }
        return values[index++]
    }

}

class IntRangeRandomizer(
    size: Int,
    range: IntRange,
    includesRangeBorders: Boolean = true,
) : RangeRandomizer<Int, IntRange>(
    size = size,
    range = range,
    includesRangeBorders = includesRangeBorders,
    generate = Random::nextInt,
)

class LongRangeRandomizer(
    size: Int,
    range: LongRange,
    includesRangeBorders: Boolean = true,
) : RangeRandomizer<Long, LongRange>(
    size = size,
    range = range,
    includesRangeBorders = includesRangeBorders,
    generate = Random::nextLong,
)
