package com.peterlaurence.trekme.core.billing.domain.model

import com.peterlaurence.trekme.util.datetime.Days
import com.peterlaurence.trekme.util.datetime.days_

/**
 * Created by Ivan Yakushev on 27.11.2024
 */
sealed interface TrialInfo

data class TrialAvailable(val duration: Days) : TrialInfo {

    companion object {

        /**
         * Trial periods are given in the form of "P1W" -> 1 week, or "P4D" -> 4 days.
         */
        operator fun invoke(period: String): TrialAvailable? {

            if (period.isEmpty()) return TrialAvailable(0.days_)

            val lowercased = period.lowercase()
            val days = runCatching {
                if (lowercased.first() != 'p') return@runCatching null

                val quantity = lowercased.filter { it.isDigit() }.toInt()
                val lastChar = lowercased.last()

                when (lastChar) {
                    'w' -> quantity * DAYS_IN_WEEK
                    'd' -> quantity
                    else -> null
                }?.days_

            }.getOrNull()

            return days?.let {
                TrialAvailable(it)
            }
        }

        private const val DAYS_IN_WEEK: Int = 7

    }

}

data object TrialUnavailable : TrialInfo
