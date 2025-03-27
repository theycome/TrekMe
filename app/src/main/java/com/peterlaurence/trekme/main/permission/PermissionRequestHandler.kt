package com.peterlaurence.trekme.main.permission

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import com.peterlaurence.trekme.util.android.MIN_PERMISSIONS_ANDROID_9_AND_BELOW
import com.peterlaurence.trekme.util.android.activity
import com.peterlaurence.trekme.util.android.hasPermission
import com.peterlaurence.trekme.util.android.hasPermissions
import com.peterlaurence.trekme.util.android.requestNearbyWifiPermission
import com.peterlaurence.trekme.util.android.requestNotificationPermission
import com.peterlaurence.trekme.util.android.shouldShowBackgroundLocPermRationale
import com.peterlaurence.trekme.util.compose.LaunchedEffectWithLifecycle
import com.peterlaurence.trekme.util.compose.LifeCycleObserver
import com.peterlaurence.trekme.util.compose.showSnackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow
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
private fun RequestLocationAndStorage(
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope,
    cancelRationaleBlock: () -> Unit,
) {

    val context = LocalContext.current
    val activity = context.activity

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grantedMap: Map<String, @JvmSuppressWildcards Boolean> ->
        if (!grantedMap.values.all { it }) {
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = context.getString(R.string.critical_perm_denied),
                    isLongDuration = true,
                    actionLabel = context.getString(R.string.ok_dialog)
                )

                if (result == SnackbarResult.ActionPerformed) {
                    activity.openAppSettings()
                }
            }
        }
    }

    WarningDialogCaller(
        content = R.string.no_storage_perm,
        onConfirm = {
            cancelRationaleBlock()
            permissionLauncher.launch(MIN_PERMISSIONS_ANDROID_9_AND_BELOW)
        },
        onDismiss = {
            cancelRationaleBlock()
            permissionLauncher.launch(MIN_PERMISSIONS_ANDROID_9_AND_BELOW)
        }
    )

}

@Composable
private fun RequestLocation(
    cancelRationaleBlock: () -> Unit,
) {

    val activity = LocalContext.current.activity

    WarningDialogCaller(
        content = R.string.no_location_perm,
        onConfirm = {
            cancelRationaleBlock()
            activity.openAppSettings()
        },
        onDismiss = {
            cancelRationaleBlock()
        }
    )

}

@Composable
private fun RequestMinimalPermissions(
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope,
) {

    var locationAndStorageRationale by remember { mutableStateOf(false) }
    if (locationAndStorageRationale) {
        RequestLocationAndStorage(
            snackbarHostState = snackbarHostState,
            scope = scope,
        ) { locationAndStorageRationale = false }
    }

    var locationRationale by remember { mutableStateOf(false) }
    if (locationRationale) {
        RequestLocation { locationRationale = false }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            locationRationale = true
        }
    }

    val context = LocalContext.current

    /**
     * Checks whether the app has permission to access fine location and (for Android < 10) to
     * write to device storage.
     * If the app does not have the requested permissions then the user will be prompted.
     */
    fun requestMinimalPermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            /* We absolutely need storage and location perm under Android 10 */
            if (!context.hasPermissions(*MIN_PERMISSIONS_ANDROID_9_AND_BELOW)) {
                locationAndStorageRationale = true
            }
        } else {
            /* On Android 10 and above, we just need the location perm */
            if (!context.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    LifeCycleObserver(
        onStart = { requestMinimalPermissions() }
    )

}

@Composable
private fun RequestBluetoothEnable(
    signalFlow: SharedFlow<Unit>,
    onEnabled: () -> Unit,
    onDisabled: () -> Unit,
) {

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        when (result.resultCode) {
            Activity.RESULT_OK -> onEnabled()
            Activity.RESULT_CANCELED -> onDisabled()
        }
    }

    LaunchedEffectWithLifecycle(signalFlow) {
        launcher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
    }

}

@Composable
private fun WarningDialogCaller(
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

private fun Activity.openAppSettings() =
    with(Intent()) {
        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        data = Uri.fromParts("package", packageName, null)
        startActivity(this)
    }
