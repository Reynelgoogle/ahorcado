package com.example.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Retorna la lista de permisos requeridos por la versión de Android del dispositivo.
 */
fun getRequiredNearbyPermissions(): Array<String> {
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.NEARBY_WIFI_DEVICES,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }
        else -> {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
            )
        }
    }
}

fun checkNearbyPermissions(context: Context): Boolean {
    val permissions = getRequiredNearbyPermissions()
    return permissions.all { perm ->
        ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
    }
}

class NearbyPermissionsState(
    val hasPermissions: Boolean,
    val requestPermissions: () -> Unit
)

@Composable
fun rememberNearbyPermissionsState(
    onPermissionsResult: (Boolean) -> Unit = {}
): NearbyPermissionsState {
    val context = LocalContext.current
    var hasPermissions by remember { mutableStateOf(checkNearbyPermissions(context)) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        hasPermissions = allGranted
        onPermissionsResult(allGranted)
    }

    val requestPermissions: () -> Unit = {
        val permissions = getRequiredNearbyPermissions()
        launcher.launch(permissions)
    }

    return remember(hasPermissions) {
        NearbyPermissionsState(
            hasPermissions = hasPermissions,
            requestPermissions = requestPermissions
        )
    }
}
