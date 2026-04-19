package com.vibevision.feature.camera

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibevision.feature.model.EmotionDetector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/* Joshua Leisman and Jared Abels
 * ViewModel responsible for handling camera image processing
 * and coordinating emotion detection using a machine learning model.
 *
 * Responsibilities:
 * - Receive bitmaps captured by the camera
 * - Persist images temporarily via [ImageRepository]
 * - Run ML inference using [EmotionDetector]
 * - Expose emotion label and confidence score to the UI
 * - Perform cleanup of temporary files
 *
 * This ViewModel follows the MVVM architecture and uses
 * StateFlow to expose reactive UI state.
 */
@HiltViewModel
class CameraViewModel @Inject constructor(
    // Application context is required for loading ML assets
    @param:dagger.hilt.android.qualifiers.ApplicationContext
    private val context: Context) : ViewModel() {

    private val tag = "CameraViewModel"

    // Lazily initialized ML detector to avoid unnecessary startup cost
    private val detector by lazy { EmotionDetector(context) }

    // Emotion label exposed to the UI (default: "Unknown")
    private val _emotion = MutableStateFlow("Unknown")
    val emotion: StateFlow<String> = _emotion

    // Confidence score (0–100) exposed to the UI (default: 0)
    private val _confidence = MutableStateFlow(0f)
    val confidence: StateFlow<Float> = _confidence

    /*
     * Called whenever the camera captures a processed [Bitmap].
     *
     * Workflow:
     * 1. Save the bitmap to internal storage
     * 2. Run ML inference on the bitmap
     * 3. Delete the file immediately if ML succeeds
     * 4. Run fallback cleanup to remove files older than 1 minute
     *
     * All work is performed inside [viewModelScope] to ensure
     * lifecycle awareness and proper cancellation.
     */
    fun onBitmapCaptured(bitmap: Bitmap) {
        viewModelScope.launch {
            Log.d(tag, "Received bitmap from CameraPreview")
            Log.d(tag, "Running ML on: $bitmap")

            // Run ML inference directly on the bitmap
            val mlSuccess = processWithML(bitmap)

            // Immediate logging
            if (mlSuccess) {
                Log.d(tag, "ML succeeded for $bitmap deleting file")
            } else {
                Log.w(tag, "ML failed for $bitmap")
            }
        }
    }

    /*
     * Runs emotion detection on the provided [Bitmap].
     *
     * Updates UI state flows with the detected emotion label
     * and confidence score if successful.
     *
     * @return true if ML processing succeeds, false otherwise
     */
    private fun processWithML(bitmap: Bitmap): Boolean {
        return try {
            val result = detector.analyze(bitmap)

            Log.d(tag, "Emotion detected: ${result.label} (${result.confidence})")

            // send results from ML to UI
            _emotion.value = result.label
            _confidence.value = result.confidence
            // return true if processing successful
            true
        } catch (e: Exception) {
            Log.e(tag, "Error during ML processing", e)
            false
        }
    }

    /*
     * Called when the ViewModel is about to be destroyed.
     *
     * Performs final cleanup by:
     * - Closing the ML detector
     * - Deleting any remaining temporary image files
     *
     * Ensures no resources or files leak beyond the session lifecycle.
     */
    override fun onCleared() {
        super.onCleared()
        detector.close()

        // log ML closing
        viewModelScope.launch {
            Log.d(tag, "ViewModel cleared")
        }
    }
}