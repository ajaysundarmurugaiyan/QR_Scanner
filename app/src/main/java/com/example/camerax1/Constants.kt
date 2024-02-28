package com.example.camerax1

import android.Manifest

object Constants {
    const val TAG = "cameraX"
    const val FILE_NAME_FORMAT = "yy-mm-dd-HH-mm-ss-SSS"
    const val REQUEST_CODE_PERMISSIONS =123
    val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
}