package com.example.vibevision.feature.camera

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository layer responsible for:
 * - Saving captured Bitmaps as PNGs
 * - Deleting files immediately (after ML success)
 * - Deleting stale files older than a configured time window
 * - Deleting all files when the ViewModel/app is closing
 *
 * Uses internal app storage: /data/user/0/<package>/files/captured_frames/
 * This location is private to the app and requires no storage permissions.
 */
@Singleton
class ImageRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    private val tag = "ImageRepository"

    // Internal app directory where all PNG captures are stored
    private val dir: File by lazy {
        File(context.filesDir, "captured_frames").apply {
            if (!exists()) mkdirs()
        }
    }

    /*
     * Saves the provided Bitmap to internal storage as a PNG.
     * Runs on Dispatchers.IO to avoid blocking the main thread.
     */
    suspend fun save(bitmap: Bitmap): File = withContext(Dispatchers.IO) {
        val name = timestampName()
        val file = File(dir, name)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        Log.d(tag, "Saved PNG: ${file.absolutePath}")
        file
    }

    // Deletes a single file; used when ML succeeds
    suspend fun delete(file: File) = withContext(Dispatchers.IO) {
        if (file.delete()) {
            Log.d(tag, "Deleted PNG (ML success): ${file.name}")
        } else {
            Log.w(tag, "Failed to delete PNG: ${file.name}")
        }
    }

    /*
     * Deletes all captured PNGs. Called during ViewModel.onCleared()
     * to ensure no temporary files remain after the app closes.
     */
    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        dir.listFiles()?.forEach { file ->
            if (file.delete()) {
                Log.d(tag, "Deleted PNG during ViewModel.onCleared(): ${file.name}")
            }
        }
    }

    /*
     * Deletes any PNG files older than the given time window (in minutes).
     * This runs each time a new frame is processed.
     * Helps prevent stale temporary files if ML fails.
     */
    suspend fun deleteOlderThan(minutes: Int) = withContext(Dispatchers.IO) {
        val cutoff = System.currentTimeMillis() - (minutes * 60_000)
        dir.listFiles()?.forEach { file ->
            if (file.lastModified() < cutoff) {
                if (file.delete()) {
                    Log.d(tag, "Deleted old PNG (> $minutes min): ${file.name}")
                }
            }
        }
    }

    // Generates timestamp-based filenames such as frame_2026-03-15_19-11-40.png
    private fun timestampName(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
        return "frame_${sdf.format(Date())}.png"
    }
}