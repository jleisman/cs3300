package com.vibevision.feature.model

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/* Jared Abels and Joshua Leisman
 * EmotionDetector
 *
 * Encapsulates a TensorFlow Lite interpreter used to perform
 * emotion classification on images.
 *
 * This class is responsible for:
 * - Loading the TFLite model from app assets
 * - Converting CameraX ImageProxy frames into Bitmaps
 * - Preprocessing Bitmaps into the model's expected input format
 * - Running inference and interpreting the output probabilities
 *
 * Model expectations:
 * - Input : float32[1, 128, 128, 1]
 *           128×128 single-channel (grayscale) image normalized to [0, 1]
 * - Output: float32[1, NUM_CLASSES]
 *           Probability scores for each emotion class
 */
class EmotionDetector(context: Context) : AutoCloseable {

    /*
     * Static configuration values tied to the trained model.
     */
    companion object {
        // Name of the TFLite model stored in the assets directory
        private const val MODEL_FILE = "model_2.tflite"

        // Required width and height of input images
        private const val INPUT_SIZE = 128

        // Number of channels expected by the model (1 = grayscale)
        private const val NUM_CHANNELS = 1

        // Number of emotion classes produced by the model output
        private const val NUM_CLASSES = 4

        /*
         * Labels corresponding to the output indices of the model.
         * The order must exactly match the training label order.
         */
        private val EMOTION_LABELS = listOf(
            "Neutral",
            "Happy",
            "Sad",
            "Anger",
        )
    }

    /*
     * Result returned after running emotion inference.
     *
     * label: The emotion with the highest confidence score
     * confidence: Confidence score for the predicted label
     * allScores: Map of all emotion labels to their raw scores
     */
    data class EmotionResult(
        val label: String,
        val confidence: Float,
        val allScores: Map<String, Float>
    )

    /*
     * TensorFlow Lite interpreter instance used to run inference.
     */
    private val interpreter: Interpreter

    init {
        // Configure interpreter options (e.g., number of threads)
        val options = Interpreter.Options().apply {
            setNumThreads(4)
        }

        // Load the model into memory and initialize the interpreter
        interpreter = Interpreter(loadModelFile(context), options)

        // Log output tensor shape for debugging and validation
        val outputTensor = interpreter.getOutputTensor(0)
        Log.d("TFLite", "Output shape = ${outputTensor.shape().contentToString()}")
    }

    /*
     * Loads the TensorFlow Lite model file from the app's assets directory
     * into a memory-mapped buffer suitable for the interpreter.
     */
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

    /*
     * Runs emotion analysis directly on an existing [Bitmap].
     *
     * This is used when the image is already available in memory
     * (e.g., from camera preview or gallery).
     */
    fun analyze(bitmap: Bitmap): EmotionResult = runInference(bitmap)

    /*
     * Preprocesses a Bitmap into the model's expected input format.
     *
     * Requirements:
     * - Bitmap must already be 128×128
     * - Image must be grayscale
     *
     * Processing:
     * - Extract pixel values
     * - Normalize intensity to [0, 1]
     * - Pack values into a direct ByteBuffer
     */
    private fun preprocessBitmap(bitmap: Bitmap): ByteBuffer {
        require(bitmap.width == INPUT_SIZE && bitmap.height == INPUT_SIZE) {
            "Bitmap must be ${INPUT_SIZE}x${INPUT_SIZE}, got ${bitmap.width}x${bitmap.height}"
        }

        val byteBuffer = ByteBuffer.allocateDirect(
            1 * INPUT_SIZE * INPUT_SIZE * NUM_CHANNELS * 4
        ).apply {
            order(ByteOrder.nativeOrder())
        }

        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap.getPixels(
            pixels,
            0,
            INPUT_SIZE,
            0,
            0,
            INPUT_SIZE,
            INPUT_SIZE
        )

        for (pixel in pixels) {
            // Grayscale image: R, G, and B channels are equivalent
            val gray = (pixel and 0xFF) / 255.0f
            byteBuffer.putFloat(gray)
        }

        return byteBuffer
    }

    /*
     * Executes model inference on a preprocessed Bitmap and
     * converts raw model output into a structured [EmotionResult].
     */
    private fun runInference(bitmap: Bitmap): EmotionResult {
        val inputBuffer = preprocessBitmap(bitmap)

        // Output tensor: [1, NUM_CLASSES]
        val outputBuffer = Array(1) { FloatArray(NUM_CLASSES) }

        interpreter.run(inputBuffer, outputBuffer)

        val scores = outputBuffer[0]

        val allScores = EMOTION_LABELS.mapIndexed { index, label ->
            label to scores[index]
        }.toMap()

        val maxIndex = scores.indices.maxByOrNull { scores[it] } ?: 0

        return EmotionResult(
            label = EMOTION_LABELS[maxIndex],
            confidence = scores[maxIndex],
            allScores = allScores
        )
    }

    /*
     * Releases interpreter resources when the detector is no longer needed.
     */
    override fun close() {
        interpreter.close()
    }
}