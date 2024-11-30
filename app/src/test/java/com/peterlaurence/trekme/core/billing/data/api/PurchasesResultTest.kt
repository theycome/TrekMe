package com.peterlaurence.trekme.core.billing.data.api

import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.peterlaurence.trekme.core.billing.data.model.PurchaseIdsMonthYear
import com.peterlaurence.trekme.core.billing.data.model.PurchaseIdsSingle
import com.peterlaurence.trekme.core.billing.data.model.PurchaseType
import com.peterlaurence.trekme.core.billing.data.model.PurchasesResult
import com.peterlaurence.trekme.core.billing.data.model.getPurchase
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
class PurchasesResultTest {

    private val purchaseAba = mock(Purchase::class.java)
    private val purchaseC = mock(Purchase::class.java)
    private val purchase23 = mock(Purchase::class.java)
    private val purchase99 = mock(Purchase::class.java)

    private val purchaseResult = PurchasesResult(
        BillingResult(),
        listOf(purchaseAba, purchaseC, purchase23, purchase99)
    )

    @BeforeTest
    fun init() {
        `when`(purchaseAba.products).thenReturn(listOf("a", "b", "A"))
        `when`(purchaseC.products).thenReturn(listOf("C"))
        `when`(purchase23.products).thenReturn(listOf("2", "3"))
        `when`(purchase99.products).thenReturn(listOf("99"))

        `when`(purchaseAba.isAcknowledged).thenReturn(true)
        `when`(purchase23.isAcknowledged).thenReturn(true)
    }

    /**
     * result = listOf("a", "b", "A", "C", "2", "3", "99")
     */
    @Test
    fun `getPurchase ONE_TIME success`() {

        val ids = listOf(
            PurchaseIdsSingle("99", ""),
            PurchaseIdsMonthYear("A", "2", "1"),
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
            PurchaseIdsSingle("B", "99"),
            PurchaseIdsMonthYear("BB", "A", "99"),
            PurchaseIdsSingle("", "2"),
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
            PurchaseIdsMonthYear("", "333", "2"),
            PurchaseIdsSingle("W", "99"),
            PurchaseIdsSingle("WWW", "a"),
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
            PurchaseIdsSingle("a", ""),
            PurchaseIdsSingle("2", "999"),
            PurchaseIdsSingle("WWW", ""),
            PurchaseIdsSingle("99", "W"),
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
            PurchaseIdsMonthYear("A", "2", "1"),
            PurchaseIdsSingle("3", ""),
            PurchaseIdsSingle("a", ""),
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
            PurchaseIdsMonthYear("C", "3", "1"),
            PurchaseIdsSingle("99", "2"),
            PurchaseIdsSingle("", "A"),
            PurchaseIdsSingle("w", "115"),
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
            PurchaseIdsMonthYear("A", "99", "3"),
            PurchaseIdsMonthYear("", "C", "a"),
            PurchaseIdsSingle("a", "A"),
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
            PurchaseIdsSingle("A", ""),
            PurchaseIdsSingle("a", ""),
            PurchaseIdsMonthYear("", "C", "_"),
            PurchaseIdsSingle("a", "99"),
            PurchaseIdsSingle("a", "aa"),
        )

        ids.forEach {
            withClue(it) {
                purchaseResult.getPurchase(PurchaseType.VALID_SUB, it) shouldBe null
            }
        }

    }

}
