package com.peterlaurence.trekme.core.billing.data.api

import android.app.Application
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase
import com.peterlaurence.trekme.core.billing.data.api.components.AnnualWithGracePeriodVerifier
import com.peterlaurence.trekme.core.billing.data.model.PurchaseIdsSingle
import com.peterlaurence.trekme.core.billing.data.model.PurchaseType
import com.peterlaurence.trekme.core.billing.data.model.SubscriptionType
import com.peterlaurence.trekme.injectMock
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argThat
import org.mockito.kotlin.inOrder
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
    private val purchaseValidOneTimeMock: Purchase = mock()
    private val purchaseValidSubMock: Purchase = mock()
    private val billingClientMock: BillingClient = mock()
    private val purchaseIdsSingleMock: PurchaseIdsSingle = mock()
    private val purchaseVerifierMock: AnnualWithGracePeriodVerifier = mock()
    private val oneTimePurchaseMock: Purchase = mock()
    private val applicationMock: Application = mock()

    @BeforeTest
    fun init() = runTest {

        whenever(billingMock.queryWhetherWeHavePurchasesAndConsumeOneTimePurchase()).thenCallRealMethod()

        billingMock.injectMock<BillingConnector, _>("connector") {
            whenever(connect()).thenReturn(true)
        }
    }

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

            billingMock.queryWhetherWeHavePurchasesAndConsumeOneTimePurchase() // invoke tested method
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

//    @Test
//    fun queryWhetherWeHavePurchasesAndConsumeOneTimePurchase() = runTest {
//
//        `when`(billingQuery.queryPurchase(PurchaseType.VALID_ONE_TIME)).thenReturn(
//            oneTimePurchaseMock
//        )
//
//        //verify(billingMock, times(1)).consumeIfNotGrantedAccess()
//
//        billingMock.queryWhetherWeHavePurchasesAndConsumeOneTimePurchase()
//
//    }

}
