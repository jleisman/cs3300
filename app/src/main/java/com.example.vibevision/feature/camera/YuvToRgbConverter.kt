package com.example.vibevision.feature.camera

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import androidx.core.graphics.createBitmap

class YuvToRgbConverter {

    fun yuvToRgb(image: ImageProxy, output: Bitmap) {
        val width = image.width
        val height = image.height

        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]

        val yBuffer = yPlane.buffer
        val uBuffer = uPlane.buffer
        val vBuffer = vPlane.buffer

        val yRowStride = yPlane.rowStride
        val yPixelStride = yPlane.pixelStride

        val uRowStride = uPlane.rowStride
        val uPixelStride = uPlane.pixelStride

        val vRowStride = vPlane.rowStride
        val vPixelStride = vPlane.pixelStride

        val argbArray = IntArray(width * height)
        var outIndex = 0

        for (row in 0 until height) {
            val yRowOffset = row * yRowStride
            val uvRowOffset = (row / 2) * uRowStride
            val vuvRowOffset = (row / 2) * vRowStride

            for (col in 0 until width) {
                val yIndex = yRowOffset + col * yPixelStride

                val uIndex = uvRowOffset + (col / 2) * uPixelStride
                val vIndex = vuvRowOffset + (col / 2) * vPixelStride

                val y = (yBuffer.get(yIndex).toInt() and 0xFF)
                val u = (uBuffer.get(uIndex).toInt() and 0xFF)
                val v = (vBuffer.get(vIndex).toInt() and 0xFF)

                var yf = y - 16
                if (yf < 0) yf = 0
                val uf = u - 128
                val vf = v - 128

                val r = (1.164f * yf + 1.596f * vf).toInt().coerceIn(0, 255)
                val g = (1.164f * yf - 0.392f * uf - 0.813f * vf).toInt().coerceIn(0, 255)
                val b = (1.164f * yf + 2.017f * uf).toInt().coerceIn(0, 255)

                argbArray[outIndex++] =
                    (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        }

        output.setPixels(argbArray, 0, width, 0, 0, width, height)
    }
}

fun ImageProxy.toBitmap(converter: YuvToRgbConverter): Bitmap {
    val bitmap = createBitmap(width, height)
    converter.yuvToRgb(this, bitmap)
    return bitmap
}