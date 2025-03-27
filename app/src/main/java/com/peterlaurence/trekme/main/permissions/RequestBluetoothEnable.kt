package com.peterlaurence.trekme.main.permissions

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import com.peterlaurence.trekme.util.compose.LaunchedEffectWithLifecycle
import kotlinx.coroutines.flow.SharedFlow

/**
 * Created by Ivan Yakushev on 27.03.2025
 */
@Composable
fun RequestBluetoothEnable(
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
