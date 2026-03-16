package com.vibevision.feature.camera

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import androidx.core.graphics.createBitmap

/*
 * Converts a CameraX YUV_420_888 frame into a grayscale ARGB_8888 Bitmap.
 *
 * This implementation uses only the Y (luma) plane, which already represents
 * brightness in an 8‑bit 0–255 range. U and V chroma planes are ignored.
 *
 * Each pixel is produced by copying the Y value into the R, G, and B channels,
 * generating a visually accurate grayscale image with minimal CPU cost.
 *
 * This approach is significantly faster than full YUV → RGB conversion and is
 * well‑suited for real‑time analysis (e.g., luma tracking, PPG extraction,
 * motion detection, or downsampled previews).
 */
class YuvToRgbConverter {
    fun yuvToGrayscale(image: ImageProxy, output: Bitmap) {
        val width = image.width
        val height = image.height

        val yPlane = image.planes[0]
        val yBuffer = yPlane.buffer

        val yRowStride = yPlane.rowStride
        val yPixelStride = yPlane.pixelStride

        val argbArray = IntArray(width * height)
        var outIndex = 0

        for (row in 0 until height) {
            val yRowOffset = row * yRowStride

            for (col in 0 until width) {
                val yIndex = yRowOffset + col * yPixelStride

                // Raw Y value as unsigned
                val y = yBuffer.get(yIndex).toInt() and 0xFF

                val gray = y.coerceIn(0, 255)

                // Pack grayscale into ARGB (same value for R, G, and B)
                argbArray[outIndex++] =
                    (0xFF shl 24) or (gray shl 16) or (gray shl 8) or gray
            }
        }

        // Write pixel buffer into provided output Bitmap
        output.setPixels(argbArray, 0, width, 0, 0, width, height)
    }
}

/*
 * Convenience extension to create a Bitmap directly from an ImageProxy.
 * Allocates an ARGB_8888 Bitmap and fills it using the converter above.
 */
fun ImageProxy.toBitmap(converter: YuvToRgbConverter): Bitmap {
    val bitmap = createBitmap(width, height)
    converter.yuvToGrayscale(this, bitmap)
    return bitmap
}