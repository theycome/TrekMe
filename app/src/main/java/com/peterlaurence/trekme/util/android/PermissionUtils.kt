package com.peterlaurence.trekme.util.android

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat

fun isBackgroundLocationGranted(appContext: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return true
    val permissionLocation = ActivityCompat.checkSelfPermission(
        appContext,
        Manifest.permission.ACCESS_BACKGROUND_LOCATION
    )
    return permissionLocation == PackageManager.PERMISSION_GRANTED
}

/**
 * prefix with `is` to avoid name clash with Activity's `shouldShowRequestPermissionRationale`
 */
fun Activity.isShowRequestPermissionRationale(permission: String): Boolean =
    ActivityCompat.shouldShowRequestPermissionRationale(this, permission)

fun Context.hasPermission(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

fun Context.hasPermissions(vararg permissions: String): Boolean =
    permissions.all(::hasPermission)

fun Context.isLocationEnabled(): Boolean =
    (getSystemService(Context.LOCATION_SERVICE) as LocationManager)
        .let(LocationManagerCompat::isLocationEnabled)

fun Context.isBatteryOptimized(): Boolean =
    (getSystemService(Context.POWER_SERVICE) as PowerManager)
        .run { isIgnoringBatteryOptimizations(packageName) }

fun Activity.openAppSettings() =
    with(Intent()) {
        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        data = Uri.fromParts("package", packageName, null)
        startActivity(this)
    }
