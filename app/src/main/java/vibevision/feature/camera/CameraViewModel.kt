package com.vibevision.feature.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.camera.core.impl.utils.ContextUtil.getApplicationContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibevision.feature.model.EmotionDetector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject



@HiltViewModel
class CameraViewModel @Inject constructor(
    private val repo: ImageRepository,
    @param:dagger.hilt.android.qualifiers.ApplicationContext
    private val context: Context
) : ViewModel() {

    private val tag = "CameraViewModel"
    private val detector by lazy { EmotionDetector(context) }

    // Emotion label exposed to the UI, defaults to "Unknown" before any ML result
    private val _emotion = MutableStateFlow("Unknown")
    val emotion: StateFlow<String> = _emotion

    // Confidence score (0–100) exposed to the UI, defaults to 0 before any ML result
    private val _confidence = MutableStateFlow(0f)
    val confidence: StateFlow<Float> = _confidence

    /*
     * Called every time CameraPreview delivers a processed Bitmap.
     *
     * Steps:
     * 1. Save the bitmap as a PNG to internal storage
     * 2. Pass the file to the ML model for inference
     * 3. Delete the file immediately if ML succeeds
     * 4. Run fallback cleanup to remove files older than 1 minute,
     *    catching any files that were not deleted in step 3
     */
    fun onBitmapCaptured(bitmap: Bitmap) {
        viewModelScope.launch {
            Log.d(tag, "Received bitmap from CameraPreview")

            // Save file
            val file = repo.save(bitmap)
            Log.d(tag, "Running ML on: ${file.name}")

            // Run ML (placeholder)
            val mlSuccess = processWithML(file)

            // Cleanup after success
            if (mlSuccess) {
                Log.d(tag, "ML succeeded for ${file.name}, deleting immediately")
                repo.delete(file)
            } else {
                Log.w(tag, "ML failed or returned false for ${file.name}")
            }

            // Delete all files older than 1 minute
            // This keeps the directory clean
            repo.deleteOlderThan(1)
            Log.d(tag, "Ran fallback cleanup (older than 1 minute)")
        }
    }


    private suspend fun processWithML(file: File): Boolean {
        return try {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return false

            val result = detector.analyze(bitmap)

            Log.d(tag, "Emotion: ${result.label} (${result.confidence})")

            true
        } catch (e: Exception) {
            Log.e(tag, "Error during ML processing", e)
            false
        }
    }

    /*
     * Called when the ViewModel is about to be destroyed, typically when
     * the Activity finishes or the app process is killed.
     *
     * Deletes all remaining PNG files from internal storage so no
     * temporary captures are left on disk after the session ends.
     */
    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            Log.d(tag, "ViewModel cleared, deleting all remaining PNGs")
            repo.deleteAll()
        }
    }
}