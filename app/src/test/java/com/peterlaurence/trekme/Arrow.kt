package com.peterlaurence.trekme

import arrow.core.raise.Raise
import arrow.core.raise.RaiseDSL
import arrow.core.raise.recover

/**
 * Created by Ivan Yakushev on 20.12.2024
 */
@RaiseDSL
inline fun <Error : Any, A> recoverAssertHappyPath(
    block: Raise<Error>.() -> A,
): A? = recover(block) {
    shouldNotHappen()
    null
}
