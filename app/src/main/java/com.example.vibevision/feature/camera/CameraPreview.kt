package com.example.vibevision.feature.camera

import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.scale
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    cameraSelector: CameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA,
    analysisIntervalMillis: Long = 5_000L,  // how long do we wait between saved images
    targetWidth: Int = 128,                 // targeted size for ML is 128x128
    targetHeight: Int = 128,
    onFrameCaptured: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraProviderFuture = remember {
        ProcessCameraProvider.getInstance(context)
    }

    val analysisExecutor = remember {
        Executors.newSingleThreadExecutor()
    }

    // Our new converter
    val yuvToRgbConverter = remember {
        YuvToRgbConverter()
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }

            val previewUseCase = Preview.Builder()
                .build()
                .also { preview ->
                    preview.surfaceProvider = previewView.surfaceProvider
                }

            val lastAnalyzedTimestamp = AtomicLong(0L)

            val analysisUseCase = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                        try {
                            val now = System.currentTimeMillis()
                            val last = lastAnalyzedTimestamp.get()

                            if (now - last >= analysisIntervalMillis) {
                                lastAnalyzedTimestamp.set(now)

                                val rotationDegrees = imageProxy.imageInfo.rotationDegrees

                                // Uses our extension + converter
                                val bitmap = imageProxy.toBitmap(yuvToRgbConverter)
                                val rotated = rotateBitmapIfNeeded(bitmap, rotationDegrees)
                                val standardized = cropCenterAndResize(
                                    source = rotated,
                                    targetWidth = targetWidth,
                                    targetHeight = targetHeight
                                )

                                onFrameCaptured(standardized)
                            }
                        } catch (t: Throwable) {
                            Log.e("CameraPreview", "Image analysis failed", t)
                        } finally {
                            imageProxy.close()
                        }
                    }
                }

            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        previewUseCase,
                        analysisUseCase
                    )
                } catch (e: Exception) {
                    Log.e("CameraPreview", "Use case binding failed", e)
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        }
    )
}

private fun rotateBitmapIfNeeded(source: Bitmap, rotationDegrees: Int): Bitmap {
    if (rotationDegrees == 0)
        return source
    else {
        val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
        return Bitmap.createBitmap(
            source,
            0,
            0,
            source.width,
            source.height,
            matrix,
            true
        )
    }
}

private fun cropCenterAndResize(
    source: Bitmap,
    targetWidth: Int,
    targetHeight: Int
): Bitmap {
    val srcWidth = source.width
    val srcHeight = source.height

    val targetAspect = targetWidth.toFloat() / targetHeight
    val srcAspect = srcWidth.toFloat() / srcHeight

    val cropWidth: Int
    val cropHeight: Int
    val cropX: Int
    val cropY: Int

    if (srcAspect > targetAspect) {
        cropHeight = srcHeight
        cropWidth = (srcHeight * targetAspect).toInt()
        cropX = (srcWidth - cropWidth) / 2
        cropY = 0
    } else {
        cropWidth = srcWidth
        cropHeight = (srcWidth / targetAspect).toInt()
        cropX = 0
        cropY = (srcHeight - cropHeight) / 2
    }

    val cropped = Bitmap.createBitmap(
        source,
        cropX.coerceAtLeast(0),
        cropY.coerceAtLeast(0),
        cropWidth.coerceAtMost(srcWidth),
        cropHeight.coerceAtMost(srcHeight)
    )

    // KTX scale for resizing
    return cropped.scale(
        width = targetWidth,
        height = targetHeight,
        filter = true
    )
}