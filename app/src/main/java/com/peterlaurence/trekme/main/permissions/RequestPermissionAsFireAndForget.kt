package com.peterlaurence.trekme.main.permissions

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
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
    val launcher = SinglePermissionLauncher(notificationId)

    LaunchedEffectWithLifecycle(signalFlow) {
        if (!launcher.hasPermission(context)) {
            launcher()
        }
    }

}
