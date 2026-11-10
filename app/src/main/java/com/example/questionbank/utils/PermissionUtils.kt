package com.example.questionbank.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

object PermissionUtils {
    private const val CAMERA_PERMISSION = Manifest.permission.CAMERA
    private const val STORAGE_PERMISSION = Manifest.permission.READ_EXTERNAL_STORAGE

    fun isCameraPermissionGranted(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, CAMERA_PERMISSION) == PackageManager.PERMISSION_GRANTED

    fun isStoragePermissionGranted(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, STORAGE_PERMISSION) == PackageManager.PERMISSION_GRANTED

    @Composable
    fun rememberCameraPermission(onGranted: () -> Unit): () -> Unit {
        val context = LocalContext.current
        var hasPermission by remember { mutableStateOf(isCameraPermissionGranted(context)) }
        val launcher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            hasPermission = granted
            if (granted) onGranted()
        }

        return {
            if (hasPermission || isCameraPermissionGranted(context)) {
                hasPermission = true
                onGranted()
            } else {
                launcher.launch(CAMERA_PERMISSION)
            }
        }
    }

    const val REQUEST_CAMERA = 1001
    const val REQUEST_STORAGE = 1002
}
