package com.peterlaurence.trekme.main.permissions

import android.Manifest
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.peterlaurence.trekme.events.AppEventBus
import com.peterlaurence.trekme.features.record.presentation.ui.components.dialogs.LocationRationale
import com.peterlaurence.trekme.util.android.activity
import com.peterlaurence.trekme.util.compose.LaunchedEffectWithLifecycle
import kotlinx.coroutines.flow.SharedFlow

/**
 * Created by Ivan Yakushev on 28.03.2025
 */
@Composable
fun RequestBackgroundLocation(
    signalFlow: SharedFlow<AppEventBus.BackgroundLocationRequest>,
    onPermissionRequestResult: (granted: Boolean) -> Unit,
) {

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return

    val context = LocalContext.current

    val launcher = SinglePermissionLauncher(
        Manifest.permission.ACCESS_BACKGROUND_LOCATION
    ) { onPermissionRequestResult(it) }

    var locationRequest: AppEventBus.BackgroundLocationRequest? by rememberSaveable {
        mutableStateOf(null)
    }

    locationRequest?.apply {
        LocationRationale(
            text = context.getString(rationaleId),
            onConfirm = { launcher() },
            onIgnore = { onPermissionRequestResult(false) },
        )
        locationRequest = null
    }

    LaunchedEffectWithLifecycle(signalFlow) {
        if (launcher.isShowRequestPermissionRationale(context.activity)) {
            locationRequest = it
        } else {
            launcher()
        }
    }

}
