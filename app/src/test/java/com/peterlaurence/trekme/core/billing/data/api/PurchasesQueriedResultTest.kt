package com.peterlaurence.trekme.core.billing.data.api

import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Created by Ivan Yakushev on 12.11.2024
 */
class PurchasesQueriedResultTest {

    private val purchaseA = mock(Purchase::class.java)
    private val purchaseC = mock(Purchase::class.java)
    private val purchase23 = mock(Purchase::class.java)
    private val purchase99 = mock(Purchase::class.java)

    private val purchaseResult = PurchasesQueriedResult(
        BillingResult(),
        listOf(purchaseA, purchaseC, purchase23, purchase99)
    )

    @BeforeTest
    fun init() {
        `when`(purchaseA.products).thenReturn(listOf("a", "b", "A"))
        `when`(purchaseC.products).thenReturn(listOf("C"))
        `when`(purchase23.products).thenReturn(listOf("2", "3"))
        `when`(purchase99.products).thenReturn(listOf("99"))

        `when`(purchaseA.isAcknowledged).thenReturn(true)
        `when`(purchase23.isAcknowledged).thenReturn(true)
    }

    /**
     * result = listOf("a", "b", "A", "C", "2", "3", "99")
     */
    @Test
    fun `getPurchase ONE_TIME success`() {

        val ids = listOf(
            PurchaseIds("A", listOf("1", "2", "3")),
            PurchaseIds("99", listOf()),
        )

        ids.forEach {
            withClue(it) {
                purchaseResult.getPurchase(PurchaseType.ONE_TIME, it) shouldNotBe null
            }
        }

    }

    /**
     * result = listOf("a", "b", "A", "C", "2", "3", "99")
     */
    @Test
    fun `getPurchase ONE_TIME failure`() {

        val ids = listOf(
            PurchaseIds("B", listOf("99")),
            PurchaseIds("BB", listOf("99")),
            PurchaseIds("", listOf("1")),
        )

        ids.forEach {
            withClue(it) {
                purchaseResult.getPurchase(PurchaseType.ONE_TIME, it) shouldBe null
            }
        }
    }

    /**
     * result = listOf("a", "b", "A", "C", "2", "3", "99")
     */
    @Test
    fun `getPurchase SUB success`() {

        val ids = listOf(
            PurchaseIds("", listOf("2", "333")),
            PurchaseIds("W", listOf("99")),
            PurchaseIds("WWW", listOf("a")),
        )

        ids.forEach {
            withClue(it) {
                purchaseResult.getPurchase(PurchaseType.SUB, it) shouldNotBe null
            }
        }

    }

    /**
     * result = listOf("a", "b", "A", "C", "2", "3", "99")
     */
    @Test
    fun `getPurchase SUB failure`() {

        val ids = listOf(
            PurchaseIds("", listOf()),
            PurchaseIds("W", listOf("999")),
            PurchaseIds("WWW", listOf("")),
            PurchaseIds("WWW", listOf("W")),
        )

        ids.forEach {
            withClue(it) {
                purchaseResult.getPurchase(PurchaseType.SUB, it) shouldBe null
            }
        }

    }

    /**
     * acknowledged result = listOf("a", "b", "A", "2", "3")
     * not acknowledged result = listOf("C", "99")
     */
    @Test
    fun `getPurchase VALID_ONE_TIME success`() {

        val ids = listOf(
            PurchaseIds("A", listOf("1", "2", "3")),
            PurchaseIds("3", listOf()),
            PurchaseIds("a", listOf("")),
        )

        ids.forEach {
            withClue(it) {
                purchaseResult.getPurchase(PurchaseType.VALID_ONE_TIME, it) shouldNotBe null
            }
        }

    }

    /**
     * acknowledged result = listOf("a", "b", "A", "2", "3")
     * not acknowledged result = listOf("C", "99")
     */
    @Test
    fun `getPurchase VALID_ONE_TIME failure`() {

        val ids = listOf(
            PurchaseIds("C", listOf("1", "2", "3")),
            PurchaseIds("99", listOf()),
            PurchaseIds("", listOf("")),
            PurchaseIds("w", listOf("115")),
        )

        ids.forEach {
            withClue(it) {
                purchaseResult.getPurchase(PurchaseType.VALID_ONE_TIME, it) shouldBe null
            }
        }

    }

    /**
     * acknowledged result = listOf("a", "b", "A", "2", "3")
     * not acknowledged result = listOf("C", "99")
     */
    @Test
    fun `getPurchase VALID_SUB success`() {

        val ids = listOf(
            PurchaseIds("A", listOf("1", "2", "3", "99")),
            PurchaseIds("", listOf("a", "C")),
            PurchaseIds("a", listOf("A")),
        )

        ids.forEach {
            withClue(it) {
                purchaseResult.getPurchase(PurchaseType.VALID_SUB, it) shouldNotBe null
            }
        }

    }

    /**
     * acknowledged result = listOf("a", "b", "A", "2", "3")
     * not acknowledged result = listOf("C", "99")
     */
    @Test
    fun `getPurchase VALID_SUB failure`() {

        val ids = listOf(
            PurchaseIds("A", listOf()),
            PurchaseIds("a", listOf("")),
            PurchaseIds("", listOf("_", "C")),
            PurchaseIds("a", listOf("99")),
            PurchaseIds("a", listOf("aa")),
        )

        ids.forEach {
            withClue(it) {
                purchaseResult.getPurchase(PurchaseType.VALID_SUB, it) shouldBe null
            }
        }

    }

}
