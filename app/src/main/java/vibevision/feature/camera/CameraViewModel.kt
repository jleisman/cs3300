package com.vibevision.feature.camera

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val repo: ImageRepository
) : ViewModel() {

    private val tag = "CameraViewModel"

    /*
     * Called every time CameraPreview delivers a processed Bitmap.
     * This function:
     * 1. Saves the bitmap as a PNG
     * 2. Runs ML on the saved file
     * 3. Performs immediate cleanup if ML succeeds
     * 4. Performs fallback cleanup (files older than 1 minute)
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

    /*
     * Stub for ML processing.
     * Replace this with your actual TensorFlow Lite inference logic.
     * This method currently always returns false to force the fallback path.
     */
    private suspend fun processWithML(file: File): Boolean {
        // TODO: Replace with actual ML logic
        return false
    }

    /*
     * Called when the ViewModel is about to be destroyed.
     * This typically happens when the Activity finishes or the app is closed.
     *
     * We perform a full cleanup here so no temporary PNGs remain on disk.
     */
    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            // Cleanup all stored images
            Log.d(tag, "ViewModel.onCleared() → deleting all PNGs")
            repo.deleteAll()
        }
    }
}