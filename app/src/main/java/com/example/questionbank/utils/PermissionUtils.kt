package com.example.questionbank.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat

object PermissionUtils {
    
    private const val CAMERA_PERMISSION = Manifest.permission.CAMERA
    private const val STORAGE_PERMISSION = Manifest.permission.READ_EXTERNAL_STORAGE
    
    /**
     * Check if camera permission is granted
     */
    fun isCameraPermissionGranted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            CAMERA_PERMISSION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Check if storage permission is granted
     */
    fun isStoragePermissionGranted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            STORAGE_PERMISSION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Request camera permission
     */
    fun requestCameraPermission(
        context: Context,
        onGranted: () -> Unit,
        onDenied: () -> Unit = {}
    ) {
        val activity = context as? Activity ?: return
        
        if (isCameraPermissionGranted(context)) {
            onGranted()
            return
        }
        
        activity.requestPermissions(
            arrayOf(CAMERA_PERMISSION),
            REQUEST_CAMERA
        )
    }
    
    const val REQUEST_CAMERA = 1001
    const val REQUEST_STORAGE = 1002
    
    @Composable
    fun rememberCameraPermission(): Boolean {
        val context = android.app.Application::getContext()
        var hasPermission by remember { mutableStateOf(isCameraPermissionGranted(context)) }
        
        val launcher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            hasPermission = isGranted
        }
        
        return hasPermission
    }
    
    fun requestCameraPermission(
        hasPermission: Boolean,
        onRequest: () -> Unit
    ) {
        if (!hasPermission) {
            onRequest()
        }
    }
}
