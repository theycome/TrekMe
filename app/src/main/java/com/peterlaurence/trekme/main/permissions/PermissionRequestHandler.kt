package com.peterlaurence.trekme.main.permissions

import androidx.annotation.StringRes
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.events.AppEventBus
import com.peterlaurence.trekme.events.gpspro.GpsProEvents
import com.peterlaurence.trekme.features.common.presentation.ui.dialogs.WarningDialog
import com.peterlaurence.trekme.util.android.activity
import com.peterlaurence.trekme.util.android.requestNearbyWifiPermission
import com.peterlaurence.trekme.util.android.requestNotificationPermission
import com.peterlaurence.trekme.util.compose.LaunchedEffectWithLifecycle
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

    // TODO - HERE
    val activity = LocalContext.current.activity

    LaunchedEffectWithLifecycle(appEventBus.requestNotificationPermFlow) {
        requestNotificationPermission(activity)
    }

    LaunchedEffectWithLifecycle(appEventBus.requestNearbyWifiDevicesPermFlow) {
        requestNearbyWifiPermission(activity)
    }
}

@Composable
fun WarningDialogCaller(
    @StringRes title: Int = R.string.warning_title,
    @StringRes content: Int,
    @StringRes confirmButton: Int = R.string.ok_dialog,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    WarningDialog(
        title = stringResource(id = title),
        contentText = stringResource(id = content),
        confirmButtonText = stringResource(id = confirmButton),
        dismissButtonText = stringResource(id = R.string.cancel_dialog_string),
        onConfirmPressed = { onConfirm() },
        onDismissRequest = { onDismiss() }
    )
}
