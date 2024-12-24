package com.peterlaurence.trekme

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import org.mockito.Mockito.mock

/**
 * Created by Ivan Yakushev on 20.12.2024
 */
fun shouldNotHappen() {
    withClue("Code execution must not reach this line") {
        true shouldBe false
    }
}

/**
 * A helper function to set up a mock and reflection-assign it to the target field
 */
inline fun <reified M, reified T> T.injectMock(intoField: String, mockInitBlock: M.() -> Unit) {
    with(mock<M>()) {
        mockInitBlock()
        this@injectMock.setPrivateProperty(intoField, this)
    }
}

/**
 * Use reflection to assign a private property
 * Could throw [NoSuchFieldException], [IllegalAccessException], [IllegalArgumentException]
 */
inline fun <reified T, P> T.setPrivateProperty(fieldName: String, property: P) {
    val klass = T::class.java
    klass.getDeclaredField(fieldName)
        .apply { isAccessible = true }
        .set(this, property)
}
