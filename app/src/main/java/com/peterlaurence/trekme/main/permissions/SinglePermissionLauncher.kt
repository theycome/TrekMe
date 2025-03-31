package com.peterlaurence.trekme.main.permissions

import android.app.Activity
import android.content.Context
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import com.peterlaurence.trekme.util.android.hasPermission
import com.peterlaurence.trekme.util.android.isShowRequestPermissionRationale

/**
 * Created by Ivan Yakushev on 31.03.2025
 *
 * ```
 *     val launcher = SinglePermissionLauncher(
 *         Manifest.permission.ACCESS_BACKGROUND_LOCATION
 *     ) { onPermissionRequestResult(it) }
 * ```
 */
class SinglePermissionLauncher private constructor(
    private val permissionId: String,
    private val launcher: ManagedActivityResultLauncher<String, Boolean>,
) {

    operator fun invoke() {
        launcher.launch(permissionId)
    }

    fun isShowRequestPermissionRationale(activity: Activity): Boolean =
        activity.isShowRequestPermissionRationale(permissionId)

    fun hasPermission(context: Context): Boolean =
        context.hasPermission(permissionId)

    companion object {

        @Composable
        operator fun invoke(
            permissionId: String,
            onResult: (Boolean) -> Unit = {},
        ) =
            SinglePermissionLauncher(
                permissionId = permissionId,
                launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) {
                    onResult(it)
                }
            )

    }

}
