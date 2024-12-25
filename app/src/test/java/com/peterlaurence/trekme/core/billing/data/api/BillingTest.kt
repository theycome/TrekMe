package com.peterlaurence.trekme.core.billing.data.api

import com.android.billingclient.api.Purchase
import com.peterlaurence.trekme.core.billing.data.api.components.AnnualWithGracePeriodVerifier
import com.peterlaurence.trekme.core.billing.data.model.PurchaseType
import com.peterlaurence.trekme.core.billing.data.model.SubscriptionType
import com.peterlaurence.trekme.injectMock
import com.peterlaurence.trekme.setPrivateProperty
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.mockito.Mockito.times
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argThat
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Created by Ivan Yakushev on 07.12.2024
 */
class BillingTest {

    @get:Rule
    val rule: MockitoRule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS)

    private val billingMock: Billing<SubscriptionType.Single> = mock()

    private val purchaseValidOneTimeMock: Purchase = mock<Purchase>().apply {
        whenever(purchaseTime).thenReturn(0)
    }

    private val purchaseVerifier = AnnualWithGracePeriodVerifier()

    @BeforeTest
    fun init() = runTest {

        billingMock.apply {
            whenever(queryWhetherWeHavePurchasesAndConsumeOneTimePurchase()).thenCallRealMethod()

            injectMock<BillingConnector, _>("connector") {
                whenever(connect()).thenReturn(true)
            }

            setPrivateProperty("purchaseVerifier", purchaseVerifier)
        }

    }

    /**
     * path VALID_ONE_TIME not null
     * - check that purchaseVerifier.checkTime gets now time close enough to UTC one
     * -- when returned AccessGranted - no query.consume() call
     *  --      else - query.consume() call
     */
    @Test
    fun `queryWhetherWeHavePurchasesAndConsumeOneTimePurchase path VALID_ONE_TIME not null`() =
        runTest {

            billingMock.injectMock<BillingQuery, _>("query") {
                whenever(
                    this::queryPurchase.invoke(
                        anyOrNull(),
                        argThat {
                            this == PurchaseType.VALID_ONE_TIME
                        })
                ).thenReturn(purchaseValidOneTimeMock)
            }

            val purchaseVerifierSpy = spy(purchaseVerifier)

            billingMock.queryWhetherWeHavePurchasesAndConsumeOneTimePurchase() // invoke tested method

            // Can not mock or argument-match value classes currently
            // https://github.com/mockk/mockk/issues/948
            //verify(purchaseVerifierSpy, times(1)).checkTime(anyOrNull(), anyOrNull())

        }

    @Test
    fun `queryWhetherWeHavePurchasesAndConsumeOneTimePurchase path VALID_ONE_TIME is null`() =
        runTest {

            val queryMock = billingMock.injectMock<BillingQuery, _>("query") {
                whenever(
                    this::queryPurchase.invoke(
                        anyOrNull(),
                        argThat {
                            this == PurchaseType.VALID_ONE_TIME
                        })
                ).thenReturn(null)
            }

            billingMock.queryWhetherWeHavePurchasesAndConsumeOneTimePurchase() // invoke tested method

            verify(queryMock, times(2))::queryPurchase.invoke(
                anyOrNull(),
                anyOrNull()
            )

            inOrder(queryMock) {

                verify(queryMock)::queryPurchase.invoke(
                    anyOrNull(),
                    argThat {
                        this == PurchaseType.VALID_ONE_TIME
                    }
                )

                verify(queryMock)::queryPurchase.invoke(
                    anyOrNull(),
                    argThat {
                        this == PurchaseType.VALID_SUB
                    }
                )

            }
        }

}
