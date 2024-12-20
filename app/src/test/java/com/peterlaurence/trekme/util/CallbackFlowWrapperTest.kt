package com.peterlaurence.trekme.util

import arrow.core.raise.recover
import com.peterlaurence.trekme.recoverAssertHappyPath
import com.peterlaurence.trekme.shouldNotHappen
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

/**
 * Created by Ivan Yakushev on 08.11.2024
 */
class CallbackFlowWrapperTest {

    @Test
    fun `vanilla usage`(): Unit = runTest {

        val v = 101
        val res = recoverAssertHappyPath {
            callbackFlowWrapper { emit ->
                emit { v }
            }()
        }

        res shouldBe v

    }

    @Test
    fun `multiple emit calls`(): Unit = runTest {

        recover({
            callbackFlowWrapper { emit ->
                emit { 0 }
                emit { 0 }
            }()
            shouldNotHappen()
        }) {
            it shouldBe CallbackFlowFailure.MultipleElementsEmitted(2)
        }

    }

    @Test
    fun `exception inside emit block`(): Unit = runTest {

        recover({
            callbackFlowWrapper { emit ->
                emit { 0 }
                error("some exception")
            }()
        }) {
            it.shouldBeTypeOf<CallbackFlowFailure.Exception>()
        }

    }

}
