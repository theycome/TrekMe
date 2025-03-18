package com.peterlaurence.trekme.util

/**
 * Created by Ivan Yakushev on 31.12.2024
 */
/**
 * Allows to obtain an iterable collection of object instances
 * representing all values present in some sealed class/interface hierarchy
 * ```
 * sealed interface MonthAndYear : SubscriptionType {
 *     data object Month : MonthAndYear
 *     data object Year : MonthAndYear
 * }
 *
 * val list: List<SubscriptionType.MonthAndYear> = sealedSubclassesInstances<SubscriptionType.MonthAndYear>()
 * ```
 */
inline fun <reified T> sealedSubclassesInstances(): List<T> =
    T::class.sealedSubclasses.map {
        it.objectInstance as T
    }.run {
        ifEmpty { listOf(T::class.objectInstance as T) }
    }
