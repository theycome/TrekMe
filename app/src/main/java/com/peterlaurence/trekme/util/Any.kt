package com.peterlaurence.trekme.util

import android.util.Log
import arrow.core.identity
import arrow.core.raise.Raise
import arrow.core.raise.RaiseDSL
import arrow.core.raise.fold
import kotlin.experimental.ExperimentalTypeInference

/**
 * Created by Ivan Yakushev on 17.11.2024
 */
fun Any.log(objekt: Any) =
    Log.i(this::class.simpleName, "$objekt - [${objekt::class.qualifiedName}]")

fun Any.logCallStack(objekt: Any) {
    log(objekt)
    Throwable("printStackTrace()").printStackTrace()
}

@OptIn(ExperimentalTypeInference::class)
@RaiseDSL
inline fun <Error : Any> Any.recoverPrintStack(
    @BuilderInference block: Raise<Error>.() -> Unit,
) = fold(block, { throw it }, { logCallStack(it) }, ::identity)
