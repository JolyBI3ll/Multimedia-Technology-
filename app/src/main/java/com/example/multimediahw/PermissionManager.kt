package com.example.multimediahw

import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionManager(
    private val activity: ComponentActivity
) {
    fun checkCameraPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                activity,
                android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    fun requestCameraPermission() {
        when {
            checkCameraPermission() -> {
                Log.i("camera_permission", "Permission previously granted")
            }

            ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                android.Manifest.permission.CAMERA
            ) -> Log.i("camera_permission", "Show permissions dialog")

            else -> {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(android.Manifest.permission.CAMERA),
                    requestCode
                )
            }
        }
    }
    companion object {
        private const val requestCode = 92
    }
}