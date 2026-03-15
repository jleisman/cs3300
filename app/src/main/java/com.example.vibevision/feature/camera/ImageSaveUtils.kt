package com.example.vibevision.feature.camera

import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

suspend fun saveBitmapAsPng(
    context: Context,
    bitmap: Bitmap,
    filename: String = generateTimestampedPngName()
): File = withContext(Dispatchers.IO) {

    val dir = File(
        context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
        "vibevision"
    )
    if (!dir.exists()) dir.mkdirs()

    val file = File(dir, filename)

    FileOutputStream(file).use { fos ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
    }

    file
}

// need this function as min APK is 24
fun generateTimestampedPngName(): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
    val timestamp = formatter.format(Date())
    return "frame_${timestamp}.png"
}