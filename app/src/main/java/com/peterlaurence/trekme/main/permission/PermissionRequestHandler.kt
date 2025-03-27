package com.peterlaurence.trekme.main.permission

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.events.AppEventBus
import com.peterlaurence.trekme.events.gpspro.GpsProEvents
import com.peterlaurence.trekme.features.common.presentation.ui.dialogs.WarningDialog
import com.peterlaurence.trekme.features.record.presentation.ui.components.dialogs.LocationRationale
import com.peterlaurence.trekme.util.android.activity
import com.peterlaurence.trekme.util.android.requestNearbyWifiPermission
import com.peterlaurence.trekme.util.android.requestNotificationPermission
import com.peterlaurence.trekme.util.android.shouldShowBackgroundLocPermRationale
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

    val context = LocalContext.current
    val activity = context.activity

    val requestBtConnectPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        gpsProEvents.postBluetoothPermissionResult(granted)
    }

    LaunchedEffectWithLifecycle(gpsProEvents.requestBluetoothPermissionFlow) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestBtConnectPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        }
    }

    var isShowingBackgroundLocationRationale by rememberSaveable {
        mutableStateOf<AppEventBus.BackgroundLocationRequest?>(
            null
        )
    }

    var backgroundLocationRequest: AppEventBus.BackgroundLocationRequest? by remember {
        mutableStateOf(
            null,
            policy = neverEqualPolicy()
        )
    }
    val backgroundLocationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        scope.launch {
            appEventBus.backgroundLocationResult.send(granted)
        }
    }

    LaunchedEffectWithLifecycle(appEventBus.requestBackgroundLocationSignal) { request ->
        if (Build.VERSION.SDK_INT < 29) return@LaunchedEffectWithLifecycle
        backgroundLocationRequest = request
        if (shouldShowBackgroundLocPermRationale(activity)) {
            isShowingBackgroundLocationRationale = request
        } else {
            backgroundLocationPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }

    isShowingBackgroundLocationRationale?.also { request ->
        LocationRationale(
            text = context.getString(request.rationaleId),
            onConfirm = {
                backgroundLocationPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                isShowingBackgroundLocationRationale = null
            },
            onIgnore = {
                scope.launch {
                    appEventBus.backgroundLocationResult.send(false)
                }
                isShowingBackgroundLocationRationale = null
            },
        )
    }

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
