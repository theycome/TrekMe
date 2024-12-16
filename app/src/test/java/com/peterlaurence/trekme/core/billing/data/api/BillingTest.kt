package com.peterlaurence.trekme.core.billing.data.api

import android.app.Application
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase
import com.peterlaurence.trekme.core.billing.data.api.components.AnnualWithGracePeriodVerifier
import com.peterlaurence.trekme.core.billing.data.model.PurchaseIdsSingle
import com.peterlaurence.trekme.core.billing.data.model.SubscriptionType
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.quality.Strictness
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Created by Ivan Yakushev on 07.12.2024
 */
class BillingTest {

    @get:Rule
    val rule: MockitoRule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS)

    @Mock
    private lateinit var billingMock: Billing<SubscriptionType.Single>

    @Mock
    private lateinit var successfulConnector: BillingConnector

    private val billingClientMock = mock(BillingClient::class.java)

    private val purchaseIdsSingleMock = mock(PurchaseIdsSingle::class.java)

    private val purchaseVerifierMock = mock(AnnualWithGracePeriodVerifier::class.java)

    private val billingQuery = BillingQuery(billingClientMock, purchaseIdsSingleMock)

    @Mock
    private lateinit var oneTimePurchaseMock: Purchase

    private val applicationMock = mock(Application::class.java)

    @BeforeTest
    fun init() = runTest {

//        val klass = Billing::class
//        val method = klass.functions.find { it.name == "connect" }!!//?.isAccessible = true

        val klass = Billing::class.java
        klass.getDeclaredField("connector")
            .apply { isAccessible = true }
            .set(billingMock, successfulConnector)

        `when`(billingMock.queryWhetherWeHavePurchasesAndConsumeOneTimePurchase()).thenCallRealMethod()
        `when`(successfulConnector.connect()).thenReturn(true)

    }

    @Test
    fun foo() = runTest {

        //val r = PowerMockito.mock(Billing::class.java)

        // TODO need a power mock to stub connect
        //`when`(billingMock.connect)

        billingMock.queryWhetherWeHavePurchasesAndConsumeOneTimePurchase()
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

class Foo {
    private var v: String = ""
}