package com.peterlaurence.trekme.util

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Created by Ivan Yakushev on 10.04.2025
 */
@OptIn(ExperimentalContracts::class)
suspend inline fun <T, R> T.letSuspend(block: suspend (T) -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return block(this)
}
