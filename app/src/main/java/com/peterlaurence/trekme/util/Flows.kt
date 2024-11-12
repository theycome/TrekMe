package com.peterlaurence.trekme.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Simple size-based chunk of a flow.
 */
fun <T> Flow<T>.chunk(maxSize: Int): Flow<List<T>> = flow {
    require(maxSize > 0) { "maxSize should be greater than 0" }

    val buffer = ArrayDeque<T>()
    collect { value ->
        buffer.add(value)
        if (buffer.size >= maxSize) {
            emit(buffer.toList())
            buffer.clear()
        }
    }

    if (buffer.isNotEmpty()) {
        emit(buffer.toList())
        buffer.clear()
    }
}

fun <T> Flow<T>.throttle(wait: Long) = channelFlow {
    val channel = Channel<T>(capacity = Channel.CONFLATED)
    coroutineScope {
        launch {
            collect {
                channel.send(it)
            }
        }
        launch {
            for (e in channel) {
                send(e)
                delay(wait)
            }
        }
    }
}

/**
 * A map implementation for StateFlow. See:
 * * https://github.com/Kotlin/kotlinx.coroutines/issues/2008
 * * https://github.com/Kotlin/kotlinx.coroutines/issues/2514
 */
fun <T, R> StateFlow<T>.map(transform: (T) -> R): StateFlow<R> = MappedStateFlow(this, transform)

/**
 * A MappedStateFlow is a StateFlow, using simple delegation mechanism.
 */
private class MappedStateFlow<T, R>(
    private val source: StateFlow<T>,
    private val mapper: (T) -> R,
) :
    StateFlow<R> {

    override val value: R
        get() = mapper(source.value)

    override val replayCache: List<R>
        get() = source.replayCache.map(mapper)

    override suspend fun collect(collector: FlowCollector<R>): Nothing {
        source.collect { value -> collector.emit(mapper(value)) }
    }
}


@Composable
fun <T> launchFlowCollectionWithLifecycle(
    flow: Flow<T>,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    minActiveState: Lifecycle.State = Lifecycle.State.RESUMED,
    flowCollector: FlowCollector<T>,
) {
    val lifeCycleAwareFlow = remember(flow, lifecycleOwner) {
        flow.flowWithLifecycle(lifecycleOwner.lifecycle, minActiveState)
    }

    LaunchedEffect(lifecycleOwner) {
        lifeCycleAwareFlow.collect(flowCollector)
    }
}

/**
 * Using a [callbackFlow] instead of [suspendCancellableCoroutine], as we have no way to remove
 * the provided callback given to async apis such as `BillingClient.queryPurchasesAsync` -
 * so creating a memory leak.
 * By collecting a [callbackFlow], the real collector is on a different call stack. So the
 * `BillingClient` in this example has no reference on the collector instance.
 *
 * ```
 * callbackFlowWrapper { emit ->
 *   client.queryPurchasesAsync(params) { billingResult, purchases ->
 *     emit {
 *       PurchasesQueriedResult(billingResult, purchases) // providing result
 *     }
 *   }
 * }()
 * ```
 *
 * @throws IllegalStateException if lambda passed into parameter `block`,
 * and eventually the invocation of `trySend`, was not performed exactly once by the client
 *
 */
suspend fun <R> callbackFlowWrapper(block: ((R) -> Unit) -> Unit): R {
    var sendCallCount = 0
    val result = callbackFlow {
        block {
            trySend(it)
            sendCallCount++
        }
        awaitClose { /* We can't do anything, but it doesn't matter */ }
    }.first()
    check(1 == sendCallCount) { "trySend() was called $sendCallCount times instead of only once" }
    return result
}
