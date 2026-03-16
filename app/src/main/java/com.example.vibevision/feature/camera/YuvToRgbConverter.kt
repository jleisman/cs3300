package com.example.vibevision.feature.camera

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import androidx.core.graphics.createBitmap

/*
 * Converts CameraX YUV_420_888 frames into ARGB Bitmaps.
 *
 * This converter reads Y, U, and V planes directly and applies
 * the standard YUV → RGB formula:
 *
 *     R = 1.164(Y-16) + 1.596(V-128)
 *     G = 1.164(Y-16) - 0.392(U-128) - 0.813(V-128)
 *     B = 1.164(Y-16) + 2.017(U-128)
 *
 * The conversion runs per pixel and is CPU-heavy, but correct and stable
 * for low‑frequency analysis (e.g., every 5s). If higher FPS is needed,
 * a GPU-backed or RenderScript alternative would be required.
 */
class YuvToRgbConverter {

    fun yuvToRgb(image: ImageProxy, output: Bitmap) {
        val width = image.width
        val height = image.height

        // CameraX YUV_420_888 planes
        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]

        val yBuffer = yPlane.buffer
        val uBuffer = uPlane.buffer
        val vBuffer = vPlane.buffer

        // Plane pixel/row strides vary by device
        val yRowStride = yPlane.rowStride
        val yPixelStride = yPlane.pixelStride

        val uRowStride = uPlane.rowStride
        val uPixelStride = uPlane.pixelStride

        val vRowStride = vPlane.rowStride
        val vPixelStride = vPlane.pixelStride

        // Output buffer for ARGB pixels
        val argbArray = IntArray(width * height)
        var outIndex = 0

        // Process every pixel in the YUV frame
        for (row in 0 until height) {
            val yRowOffset = row * yRowStride
            val uvRowOffset = (row / 2) * uRowStride
            val vuvRowOffset = (row / 2) * vRowStride

            for (col in 0 until width) {
                // Y, U, V indices (account for pixel stride)
                val yIndex = yRowOffset + col * yPixelStride
                val uIndex = uvRowOffset + (col / 2) * uPixelStride
                val vIndex = vuvRowOffset + (col / 2) * vPixelStride

                // Raw YUV values as unsigned
                val y = (yBuffer.get(yIndex).toInt() and 0xFF)
                val u = (uBuffer.get(uIndex).toInt() and 0xFF)
                val v = (vBuffer.get(vIndex).toInt() and 0xFF)

                // Normalize values per YUV formula
                var yf = y - 16
                if (yf < 0) yf = 0
                val uf = u - 128
                val vf = v - 128

                // Convert to RGB (clamp each to [0, 255])
                val r = (1.164f * yf + 1.596f * vf).toInt().coerceIn(0, 255)
                val g = (1.164f * yf - 0.392f * uf - 0.813f * vf).toInt().coerceIn(0, 255)
                val b = (1.164f * yf + 2.017f * uf).toInt().coerceIn(0, 255)

                // Pack into ARGB
                argbArray[outIndex++] =
                    (0xFF shl 24) or (r shl 16) or (g shl 8) or b
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
    converter.yuvToRgb(this, bitmap)
    return bitmap
}