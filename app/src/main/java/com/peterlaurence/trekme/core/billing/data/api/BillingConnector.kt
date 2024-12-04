package com.peterlaurence.trekme.core.billing.data.api

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode.OK
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.peterlaurence.trekme.util.datetime.Millis
import kotlinx.coroutines.delay

/**
 * Created by Ivan Yakushev on 14.11.2024
 */
/**
 * Encapsulates connection functionality
 */
class BillingConnector(private val billingClient: BillingClient) {

    private var connected = false

    private val connectionStateListener = object : BillingClientStateListener {
        override fun onBillingSetupFinished(billingResult: BillingResult) {
            connected = billingResult.responseCode == OK
        }

        override fun onBillingServiceDisconnected() {
            connected = false
        }
    }

    suspend fun connect(): Boolean =
        runCatching {
            awaitConnect(
                TEN_SECONDS,
                TEN_MILLIS,
            )
        }.isSuccess

    /**
     * Suspends at most 10s (waits for billing client to connect).
     * Since the [BillingClient] can only notify its state through the [connectionStateListener], we
     * poll the [connected] status. Ideally, we would collect the billing client state flow...
     */
    private suspend fun awaitConnect(
        totalWaitTime: Millis,
        delayTime: Millis,
    ) {
        connectClient()

        var awaited = Millis(0)
        while (awaited < totalWaitTime) {
            if (connected) {
                break
            } else {
                delay(delayTime.long)
                awaited += delayTime
            }
        }
    }

    /**
     * Attempts to connect the billing service. This function immediately returns.
     * See also [awaitConnect], which suspends at most for 10s.
     * Don't try to make this a suspend function - the [billingClient] keeps a reference on the
     * [BillingClientStateListener] so it would keep a reference on a continuation (leading to
     * insidious memory leaks, depending on who invokes that suspending function).
     * Done this way, we're sure that the [billingClient] only has a reference on this [Billing]
     * instance.
     */
    private fun connectClient() {
        if (billingClient.isReady) {
            connected = true
        } else {
            billingClient.startConnection(connectionStateListener)
        }
    }

    companion object {
        private val TEN_SECONDS = Millis(10_000)
        private val TEN_MILLIS = Millis(10)
    }

}

