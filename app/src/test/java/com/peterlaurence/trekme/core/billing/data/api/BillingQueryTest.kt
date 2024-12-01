package com.peterlaurence.trekme.core.billing.data.api

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetailsResponseListener
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesResponseListener
import com.android.billingclient.api.QueryPurchasesParams
import com.peterlaurence.trekme.core.billing.data.model.PurchaseIdsContract
import com.peterlaurence.trekme.core.billing.data.model.PurchaseIdsMonthYear
import com.peterlaurence.trekme.core.billing.data.model.PurchaseIdsSingle
import com.peterlaurence.trekme.core.billing.data.model.PurchaseType
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.quality.Strictness
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Created by Ivan Yakushev on 27.11.2024
 */
class BillingQueryTest {

    @get:Rule
    val rule: MockitoRule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS)

    private val singleOneTimeId = "single_one_time"
    private val singleSubId = "single_sub"

    private val purchaseIdsSingle = PurchaseIdsSingle(
        oneTimeId = singleOneTimeId,
        subId = singleSubId
    )

    private val monthYearOneTimeId = "my_one_time"
    private val monthYearSubYearId = "my_year"
    private val monthYearSubMonthId = "my_month"

    private val purchaseIdsMonthYear = PurchaseIdsMonthYear(
        oneTimeId = monthYearOneTimeId,
        subIdYear = monthYearSubYearId,
        subIdMonth = monthYearSubMonthId
    )

    private val purchaseOneTimeValidMock = mock(Purchase::class.java)
    private val purchaseOneTimeInvalidMock = mock(Purchase::class.java)
    private val purchaseMonthYearValidMock = mock(Purchase::class.java)
    private val purchaseMonthYearInvalidMock = mock(Purchase::class.java)

    private fun initPurchaseMocks() {
        `when`(purchaseOneTimeValidMock.products).thenReturn(listOf(singleOneTimeId, singleSubId))
        `when`(purchaseOneTimeValidMock.isAcknowledged).thenReturn(true)

        `when`(purchaseOneTimeInvalidMock.products).thenReturn(listOf(singleOneTimeId, singleSubId))
        `when`(purchaseOneTimeInvalidMock.isAcknowledged).thenReturn(false)

        `when`(purchaseMonthYearValidMock.products).thenReturn(
            listOf(
                monthYearOneTimeId,
                monthYearSubYearId,
                monthYearSubMonthId
            )
        )
        `when`(purchaseMonthYearValidMock.isAcknowledged).thenReturn(true)

        `when`(purchaseMonthYearInvalidMock.products).thenReturn(
            listOf(
                monthYearOneTimeId,
                monthYearSubYearId,
                monthYearSubMonthId
            )
        )
        `when`(purchaseMonthYearInvalidMock.isAcknowledged).thenReturn(false)
    }

    private val productDetailsSingleSubMock = mock(ProductDetails::class.java)

    private fun initProductDetailsMock() {
        `when`(productDetailsSingleSubMock.productId).thenReturn(singleSubId)
    }

    @Mock
    private lateinit var billingClientMock: BillingClient

    @Mock
    private lateinit var billingResultMock: BillingResult

    @BeforeTest
    fun init() {
        initPurchaseMocks()
        initProductDetailsMock()
    }

    @Captor
    private lateinit var queryPurchasesParamsCaptor: ArgumentCaptor<QueryPurchasesParams>

    private val purchaseTypeToApiStringMap = mapOf(
        PurchaseType.ONE_TIME to ProductType.INAPP,
        PurchaseType.SUB to ProductType.SUBS,
        PurchaseType.VALID_ONE_TIME to ProductType.INAPP,
        PurchaseType.VALID_SUB to ProductType.SUBS,
    )

    @Test
    fun `queryPurchasesResult runner`() {

        queryPurchasesResult(
            purchaseIdsSingle,
            listOf(
                PurchaseType.ONE_TIME,
                PurchaseType.VALID_ONE_TIME,
                PurchaseType.SUB,
                PurchaseType.VALID_SUB
            ),
            listOf(purchaseOneTimeValidMock, purchaseOneTimeInvalidMock),
        )

        queryPurchasesResult(
            purchaseIdsMonthYear,
            listOf(
                PurchaseType.ONE_TIME,
                PurchaseType.VALID_ONE_TIME,
                PurchaseType.SUB,
                PurchaseType.VALID_SUB
            ),
            listOf(purchaseOneTimeValidMock, purchaseOneTimeInvalidMock),
        )

    }

    private fun queryPurchasesResult(
        idsContract: PurchaseIdsContract,
        purchaseTypes: List<PurchaseType>,
        purchaseMocks: List<Purchase>,
    ) = runTest {
        purchaseTypes.forEach { purchaseType ->
            purchaseMocks.forEach { purchase ->
                withClue("$idsContract | $purchaseType | $purchase") {

                    val query = BillingQuery(billingClientMock, idsContract)

                    // mock billingClient.queryPurchasesAsync to return PurchasesResult(any, list<Purchase>)
                    doAnswer { invocation ->
                        val listener: PurchasesResponseListener = invocation.getArgument(1)
                        listener.onQueryPurchasesResponse(billingResultMock, listOf(purchase))
                    }.`when`(billingClientMock)
                        .queryPurchasesAsync(queryPurchasesParamsCaptor.capture(), any())

                    // invoke BillingQuery.queryPurchase()
                    // assert whether the correct Purchase is returned
                    query.queryPurchasesResult(purchaseType).purchases shouldContain purchase

                    // verify that a correct product type is passed to billingClient.queryPurchasesAsync(params)
                    queryPurchasesParamsCaptor.value.zza() shouldBe purchaseTypeToApiStringMap[purchaseType]
                }
            }
        }
    }

    @Test
    fun `queryProductDetailsResult check callback working`() = runTest {

        val query = BillingQuery(billingClientMock, purchaseIdsSingle)

        doAnswer { invocation ->
            val listener: ProductDetailsResponseListener = invocation.getArgument(1)
            listener.onProductDetailsResponse(
                billingResultMock,
                listOf(productDetailsSingleSubMock)
            )
        }.`when`(billingClientMock)
            .queryProductDetailsAsync(any(), any())

        query.queryProductDetailsResult("any").productDetails shouldContain productDetailsSingleSubMock
    }

}
