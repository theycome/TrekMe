package com.peterlaurence.trekme.main.permissions

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.util.android.MIN_PERMISSIONS_ANDROID_9_AND_BELOW
import com.peterlaurence.trekme.util.android.activity
import com.peterlaurence.trekme.util.android.hasPermission
import com.peterlaurence.trekme.util.android.hasPermissions
import com.peterlaurence.trekme.util.android.openAppSettings
import com.peterlaurence.trekme.util.compose.LifeCycleObserver
import com.peterlaurence.trekme.util.compose.showSnackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Created by Ivan Yakushev on 27.03.2025
 */
@Composable
fun RequestMinimalPermissions(
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
