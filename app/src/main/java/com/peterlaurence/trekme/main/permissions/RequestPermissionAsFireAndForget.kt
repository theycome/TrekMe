package com.peterlaurence.trekme.main.permissions

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.peterlaurence.trekme.util.android.hasPermission
import com.peterlaurence.trekme.util.compose.LaunchedEffectWithLifecycle
import kotlinx.coroutines.flow.SharedFlow

/**
 * Created by Ivan Yakushev on 30.03.2025
 */
@Composable
fun RequestPermissionAsFireAndForget(
    signalFlow: SharedFlow<Unit>,
    sinceSdk: Int,
    notificationId: String,
) {

    if (Build.VERSION.SDK_INT < sinceSdk) return

    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {}

    LaunchedEffectWithLifecycle(signalFlow) {

        notificationId.also {
            if (!context.hasPermission(it)) {
                launcher.launch(it)
            }
        }

    }

}
