package com.vibevision.feature.model
import java.nio.ByteBuffer
import java.nio.ByteOrder
import android.graphics.Bitmap
import androidx.core.graphics.scale
import androidx.core.graphics.get

import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.channels.FileChannel
import android.content.Context

// add
// aaptOptions{
//     noCompress("tflite")
// }
// // TFLite File
//    implementation("org.tensorflow:tensorflow-lite:2.17.0")

fun preprocessImage(bitmap: Bitmap): ByteBuffer {
    // Resize to 128x128
    val resized = bitmap.scale(128, 128)

    // 1 batch × 128 height × 128 width × 1 channel (grayscale) × 4 bytes (float)
    val byteBuffer = ByteBuffer.allocateDirect(1 * 128 * 128 * 1 * 4)
    byteBuffer.order(ByteOrder.nativeOrder())

    for (y in 0 until 128) {
        for (x in 0 until 128) {
            val pixel = resized[x, y]

            // Extract the red channel (R == G == B for grayscale)
            val r = (pixel shr 16) and 0xFF

            // Normalize to [0, 1] — adjust if your model expects a different range
            byteBuffer.putFloat(r / 255.0f)
        }
    }

    return byteBuffer
}

fun loadModel(context: Context): Interpreter {
    val assetFileDescriptor = context.assets.openFd("model.tflite")
    val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
    val fileChannel = fileInputStream.channel
    val mappedByteBuffer = fileChannel.map(
        FileChannel.MapMode.READ_ONLY,
        assetFileDescriptor.startOffset,
        assetFileDescriptor.declaredLength
    )
    return Interpreter(mappedByteBuffer)
}

fun runInference(context: Context, bitmap: Bitmap) {
    val interpreter = loadModel(context)
    val input = preprocessImage(bitmap)

    // Shape your output array to match your model's output tensor
    // Example: a single classification score array with 10 classes
    val output = Array(1) { FloatArray(10) }

    interpreter.run(input, output)

    // Use output[0] — e.g., argmax for the predicted class
    val predictedClass = output[0].indices.maxByOrNull { output[0][it] }
    println("Predicted class: $predictedClass")
}