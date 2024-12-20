package com.peterlaurence.trekme.util

import android.util.Log
import arrow.core.raise.Raise
import arrow.core.raise.RaiseDSL
import arrow.core.raise.recover

/**
 * Created by Ivan Yakushev on 17.11.2024
 */
fun Any.log(objekt: Any) =
    Log.i(this::class.simpleName, "$objekt - [${objekt::class.qualifiedName}]")

fun Any.logCallStack(objekt: Any) {
    log(objekt)
    Throwable("printStackTrace()").printStackTrace()
}

/**
 * Calls recover
 * - on failure - logs call stack and returns null
 */
@RaiseDSL
inline fun <Error : Any, A> Any.recoverLogged(
    block: Raise<Error>.() -> A,
): A? = recover(block) {
    logCallStack(it)
    null
}
