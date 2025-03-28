package com.peterlaurence.trekme.main.permissions

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import com.peterlaurence.trekme.util.compose.LaunchedEffectWithLifecycle
import kotlinx.coroutines.flow.SharedFlow

/**
 * Created by Ivan Yakushev on 27.03.2025
 */
@Composable
fun RequestBluetoothConnect(
    signalFlow: SharedFlow<Unit>,
    onResult: (Boolean) -> Unit,
) {

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        onResult(granted)
    }

    LaunchedEffectWithLifecycle(signalFlow) {
        launcher.launch(Manifest.permission.BLUETOOTH_CONNECT)
    }

}
