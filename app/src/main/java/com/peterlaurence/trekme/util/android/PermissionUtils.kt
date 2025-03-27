package com.peterlaurence.trekme.util.android

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.PowerManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat

// TODO - remove request codes as part of deprecated api
/* Permission-group codes */
//const val REQUEST_LOCATION = 1

const val REQUEST_NOTIFICATION = 4

const val REQUEST_NEARBY_WIFI = 5

val MIN_PERMISSIONS_ANDROID_9_AND_BELOW = arrayOf(
    Manifest.permission.READ_EXTERNAL_STORAGE,
    Manifest.permission.WRITE_EXTERNAL_STORAGE,
    Manifest.permission.ACCESS_FINE_LOCATION,
)

fun isBackgroundLocationGranted(appContext: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return true
    val permissionLocation = ActivityCompat.checkSelfPermission(
        appContext,
        Manifest.permission.ACCESS_BACKGROUND_LOCATION
    )
    return permissionLocation == PackageManager.PERMISSION_GRANTED
}

fun shouldShowBackgroundLocPermRationale(activity: Activity): Boolean {
    return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        false
    } else {
        ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        )
    }
}

fun requestNotificationPermission(activity: Activity) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val permission = ActivityCompat.checkSelfPermission(
        activity,
        Manifest.permission.POST_NOTIFICATIONS
    )
    if (permission != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            REQUEST_NOTIFICATION
        )
    }
}

fun requestNearbyWifiPermission(activity: Activity) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val permission = ActivityCompat.checkSelfPermission(
        activity,
        Manifest.permission.NEARBY_WIFI_DEVICES
    )
    if (permission != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.NEARBY_WIFI_DEVICES),
            REQUEST_NEARBY_WIFI
        )
    }
}

fun Context.hasPermission(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

fun Context.hasPermissions(permissions: List<String>): Boolean =
    permissions.all(::hasPermission)

fun Context.hasAnyOfPermissions(permissions: List<String>): Boolean =
    permissions.any(::hasPermission)

fun Context.hasPermissions(vararg permissions: String): Boolean =
    permissions.all(::hasPermission)

fun Context.isLocationEnabled(): Boolean =
    (getSystemService(Context.LOCATION_SERVICE) as LocationManager)
        .let(LocationManagerCompat::isLocationEnabled)

fun Context.isBatteryOptimized(): Boolean =
    (getSystemService(Context.POWER_SERVICE) as PowerManager)
        .run { isIgnoringBatteryOptimizations(packageName) }
