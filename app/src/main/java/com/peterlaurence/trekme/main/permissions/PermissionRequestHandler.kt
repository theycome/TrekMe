package com.peterlaurence.trekme.main.permissions

import android.Manifest
import android.os.Build
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import com.peterlaurence.trekme.events.AppEventBus
import com.peterlaurence.trekme.events.gpspro.GpsProEvents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun PermissionRequestHandler(
    appEventBus: AppEventBus,
    gpsProEvents: GpsProEvents,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope,
) {

    RequestMinimalPermissions(
        snackbarHostState = snackbarHostState,
        scope = scope
    )

    RequestBluetoothEnable(
        signalFlow = appEventBus.requestBluetoothEnableFlow,
        onEnabled = { appEventBus.bluetoothEnabled(true) },
        onDisabled = { appEventBus.bluetoothEnabled(false) }
    )

    RequestBluetoothConnect(gpsProEvents.requestBluetoothPermissionFlow) { granted ->
        gpsProEvents.postBluetoothPermissionResult(granted)
    }

    RequestBackgroundLocation(appEventBus.requestBackgroundLocationSignal) { granted ->
        scope.launch {
            appEventBus.backgroundLocationResult.send(granted)
        }
    }

    RequestPermissionAsFireAndForget(
        appEventBus.requestNotificationPermFlow,
        Build.VERSION_CODES.TIRAMISU,
        Manifest.permission.POST_NOTIFICATIONS
    )

    RequestPermissionAsFireAndForget(
        appEventBus.requestNearbyWifiDevicesPermFlow,
        Build.VERSION_CODES.TIRAMISU,
        Manifest.permission.NEARBY_WIFI_DEVICES
    )

}
