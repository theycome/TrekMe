package com.peterlaurence.trekme.util

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

/**
 * Created by Ivan Yakushev on 08.11.2024
 */
class CallbackFlowWrapperTest {

    @Test
    fun `vanilla usage`(): Unit = runTest {

        val v = 101
        val res = callbackFlowWrapper { emit ->
            emit { v }
        }()

        res shouldBe v
    }

    @Test
    fun `multiple emit calls`(): Unit = runTest {

        shouldThrow<IllegalStateException> {
            callbackFlowWrapper { emit ->
                emit { 0 }
                emit { 0 }
            }()
        }

    }

}
