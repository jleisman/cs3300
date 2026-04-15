package com.vibevision.feature.model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.ImageProxy
import org.tensorflow.lite.Interpreter
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import androidx.core.graphics.scale

/**
 * EmotionDetector
 *
 * Wraps the TFLite emotion model. The model expects:
 *   Input  : float32[1, 128, 128, 1]  — single-channel (grayscale), normalized to [0, 1]
 *   Output : float32[1, 8]            — softmax scores for 8 emotion classes
 *
 * Usage:
 *   val detector = EmotionDetector(context)
 *   // From a CameraX ImageProxy (call inside ImageAnalysis.Analyzer):
 *   val result = detector.analyze(imageProxy)
 *   Log.d("Emotion", result.label)        // e.g. "Happy"
 *   Log.d("Emotion", "${result.confidence}") // e.g. 0.92
 */
class EmotionDetector(context: Context) : AutoCloseable {

    // Constants
    companion object {
        private const val MODEL_FILE   = "AllVibesNoVision.tflite"
        private const val INPUT_SIZE   = 128          // model expects 128×128
        private const val NUM_CHANNELS = 1            // grayscale
        private const val NUM_CLASSES  = 8

        // Adjust this list to match the exact training-label order of your model.
        // Common 8-class FER mappings (FER+ / AffectNet):
        private val EMOTION_LABELS = listOf(
            "Neutral",
            "Happy",
            "Sad",
            "Surprise",
            "Fear",
            "Disgust",
            "Anger",
            "Contempt"
        )
    }

    // Data class returned to callers

    data class EmotionResult(
        val label: String,
        val confidence: Float,
        val allScores: Map<String, Float>  // full probability distribution
    )

    // TFLite interpreter
    private val interpreter: Interpreter

    init {
        interpreter = Interpreter(loadModelFile(context), Interpreter.Options().apply {
            setNumThreads(4)
        })
    }

    /** Load the model from the app's assets folder. */
    private fun loadModelFile(context: Context): MappedByteBuffer {
        val assetFileDescriptor = context.assets.openFd(MODEL_FILE)
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        return fileChannel.map(
            FileChannel.MapMode.READ_ONLY,
            assetFileDescriptor.startOffset,
            assetFileDescriptor.declaredLength
        )
    }

    // Public API — analyze a CameraX ImageProxy

    fun analyze(imageProxy: ImageProxy, rotationDegrees: Int = 0): EmotionResult {
        val bitmap = imageProxy.toBitmap(rotationDegrees)
        return runInference(bitmap)
    }

    /**
     * Run inference directly on any [Bitmap] (e.g. from a file or gallery pick).
     */
    fun analyze(bitmap: Bitmap): EmotionResult = runInference(bitmap)

    // Image preprocessing

    /**
     * Convert a CameraX YUV [ImageProxy] → upright RGB [Bitmap].
     */
    private fun ImageProxy.toBitmap(rotationDegrees: Int): Bitmap {
        val yBuffer = planes[0].buffer
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, width, height), 95, out)
        val jpegBytes = out.toByteArray()
        val rawBitmap = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)

        // Rotate to upright orientation
        return if (rotationDegrees != 0) {
            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
        } else {
            rawBitmap
        }
    }

    /**
     * Resize to 128×128, convert to grayscale, normalize to [0, 1],
     * and pack into a [ByteBuffer] shaped [1, 128, 128, 1].
     */
    private fun preprocessBitmap(bitmap: Bitmap): ByteBuffer {
        // 1. Resize
        val resized = bitmap.scale(INPUT_SIZE, INPUT_SIZE)

        // 2. Allocate: 1 image × 128 × 128 × 1 channel × 4 bytes (float32)
        val byteBuffer = ByteBuffer.allocateDirect(
            1 * INPUT_SIZE * INPUT_SIZE * NUM_CHANNELS * 4
        ).apply { order(ByteOrder.nativeOrder()) }

        // 3. Convert each pixel to grayscale float in [0, 1]
        val intPixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        resized.getPixels(intPixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)

        for (pixel in intPixels) {
            val r = (pixel shr 16 and 0xFF)
            val g = (pixel shr 8  and 0xFF)
            val b = (pixel        and 0xFF)
            // Standard luma (BT.601) — matches most FER training pipelines
            val gray = (0.299f * r + 0.587f * g + 0.114f * b) / 255.0f
            byteBuffer.putFloat(gray)
        }

        return byteBuffer
    }

    // Inference

    private fun runInference(bitmap: Bitmap): EmotionResult {
        val inputBuffer = preprocessBitmap(bitmap)

        // Output buffer: float32[1, 8]
        val outputBuffer = Array(1) { FloatArray(NUM_CLASSES) }

        interpreter.run(inputBuffer, outputBuffer)

        val scores = outputBuffer[0]

        // Build label → score map
        val allScores = EMOTION_LABELS.mapIndexed { i, label ->
            label to scores[i]
        }.toMap()

        // Pick the highest-confidence class
        val maxIndex = scores.indices.maxByOrNull { scores[it] } ?: 0
        return EmotionResult(
            label      = EMOTION_LABELS[maxIndex],
            confidence = scores[maxIndex],
            allScores  = allScores
        )
    }

    // Lifecycle

    override fun close() {
        interpreter.close()
    }
}

