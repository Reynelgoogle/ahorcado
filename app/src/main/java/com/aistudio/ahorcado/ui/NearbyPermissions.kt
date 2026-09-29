package com.aistudio.ahorcado.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Permisos que necesita Nearby Connections según la versión de Android.
 */
fun nearbyPermissions(): Array<String> = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_ADVERTISE,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.NEARBY_WIFI_DEVICES,
        Manifest.permission.ACCESS_FINE_LOCATION,
    )
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> arrayOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_ADVERTISE,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.ACCESS_FINE_LOCATION,
    )
    else -> arrayOf(
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.ACCESS_FINE_LOCATION,
    )
}

fun hasNearbyPermissions(context: Context): Boolean =
    nearbyPermissions().all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

/**
 * Devuelve una acción que pide los permisos de Nearby y ejecuta [onGranted]
 * solo cuando están todos otorgados. Usa el valor más reciente de [onGranted]
 * para no capturar estado viejo.
 */
@Composable
fun rememberNearbyPermissionRequest(onGranted: () -> Unit): () -> Unit {
    val context = LocalContext.current
    val latestOnGranted by rememberUpdatedState(onGranted)

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        if (results.values.all { it }) {
            latestOnGranted()
        }
    }

    return {
        if (hasNearbyPermissions(context)) {
            latestOnGranted()
        } else {
            launcher.launch(nearbyPermissions())
        }
    }
}
