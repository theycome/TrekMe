package com.peterlaurence.trekme.core.billing.domain.repositories.interactors

import arrow.core.mapOrAccumulate
import arrow.core.raise.Raise
import com.peterlaurence.trekme.core.billing.data.model.SubscriptionDetails
import com.peterlaurence.trekme.core.billing.data.model.SubscriptionType

/**
 * Created by Ivan Yakushev on 04.01.2025
 *
 * Initialized with a following snippet
 * ```kotlin
 *         SubscriptionInteractor(
 *             types = sealedSubclassesInstances<SubscriptionType.Single>(),
 *             providersMap = mapOf(
 *                 SubscriptionType.Single to SubscriptionDetailsProvider(
 *                     producer = { subDetailsFlow.value },
 *                     consumer = { _subDetailsFlow.value = it },
 *                 )
 *             ),
 *         )
 * ```
 */
class SubscriptionInteractor<T : SubscriptionType>(
    val types: List<T>,
    val providersMap: Map<T, SubscriptionDetailsProvider>,
) {

    /**
     * Check whether `providersMap` was initialized properly by the client.
     * That is - for each T the corresponding SubscriptionDetailsProvider was specified.
     * Throws `IllegalStateException` when such a failure is detected
     */
    init {
        types.mapOrAccumulate { type ->
            checkInteractor(type)
        }.onLeft {
            error(it)
        }
    }

    /**
     * Assume that if we got here then we have the class being successfully initialized.
     * Therefore, we may always rely on safely querying interactors by type
     */
    operator fun invoke(type: T, block: (SubscriptionDetailsProvider) -> Unit) {
        providersMap[type]?.run {
            block(this)
        }
    }

    context(Raise<GetSubscriptionTypeFailure<T>>)
    private fun checkInteractor(type: T) {
        if (providersMap[type] == null) {
            raise(GetSubscriptionTypeFailure.TypeNotFoundInMap(type))
        }
    }

    data class SubscriptionDetailsProvider(
        val producer: () -> SubscriptionDetails?,
        val consumer: (SubscriptionDetails) -> Unit,
    )

    private sealed interface GetSubscriptionTypeFailure<T : SubscriptionType> {
        data class TypeNotFoundInMap<T : SubscriptionType>(val type: T) :
            GetSubscriptionTypeFailure<T>
    }

}
