package com.peterlaurence.trekme.main.permissions

import android.Manifest
import android.os.Build
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

    val launcher = SinglePermissionLauncher(
        Manifest.permission.BLUETOOTH_CONNECT
    ) { onResult(it) }

    LaunchedEffectWithLifecycle(signalFlow) {
        launcher()
    }

}
