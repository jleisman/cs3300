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

/**
 * Displays a CameraX PreviewView inside a Compose layout and
 * captures frames at a fixed interval for ML processing.
 *
 * Responsibilities:
 * - Host CameraX Preview inside Compose using AndroidView
 * - Configure ImageAnalysis and throttle frame capture (every N ms)
 * - Convert ImageProxy → Bitmap, rotate, crop, resize
 * - Deliver standardized Bitmap to the ViewModel
 */
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    cameraSelector: CameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA,
    analysisIntervalMillis: Long = 5_000L,  // Minimum time between captures
    targetWidth: Int = 128,                 // Final ML input size
    targetHeight: Int = 128,
    onFrameCaptured: (Bitmap) -> Unit       // Callback to ViewModel
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // CameraProvider is expensive; remember it across recompositions
    val cameraProviderFuture = remember {
        ProcessCameraProvider.getInstance(context)
    }

    // Single-thread executor ensures ImageAnalysis frames are processed sequentially
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    // Converter from YUV_420_888 (ImageProxy) → ARGB Bitmap
    val yuvToRgbConverter = remember { YuvToRgbConverter() }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            // Standard CameraX Preview rendering surface
            val previewView = PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }

            // CameraX Preview use case
            val previewUseCase = Preview.Builder()
                .build()
                .also { preview ->
                    preview.surfaceProvider = previewView.surfaceProvider
                }

            // Used to throttle ImageAnalysis events
            val lastAnalyzedTimestamp = AtomicLong(0L)

            // ImageAnalysis use case for reading frames
            val analysisUseCase = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                        try {
                            val now = System.currentTimeMillis()
                            val last = lastAnalyzedTimestamp.get()

                            // Only process a frame if enough time has elapsed
                            if (now - last >= analysisIntervalMillis) {
                                lastAnalyzedTimestamp.set(now)

                                val rotationDegrees = imageProxy.imageInfo.rotationDegrees

                                // Convert YUV → Bitmap
                                val bitmap = imageProxy.toBitmap(yuvToRgbConverter)

                                // Ensure orientation matches screen
                                val rotated = rotateBitmapIfNeeded(bitmap, rotationDegrees)

                                // Center‑crop and resize to ML‑target resolution
                                val standardized = cropCenterAndResize(
                                    source = rotated,
                                    targetWidth = targetWidth,
                                    targetHeight = targetHeight
                                )

                                // Deliver standardized bitmap to the caller (ViewModel)
                                onFrameCaptured(standardized)
                            }
                        } catch (t: Throwable) {
                            Log.e("CameraPreview", "Image analysis failed", t)
                        } finally {
                            // Must close each ImageProxy or CameraX stalls
                            imageProxy.close()
                        }
                    }
                }

            // Bind preview + analysis to lifecycle
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

/**
 * Rotates a bitmap using the rotation degrees provided by CameraX.
 *
 * CameraX delivers images rotated depending on device orientation.
 * ML models expect normalized upright images.
 */
private fun rotateBitmapIfNeeded(source: Bitmap, rotationDegrees: Int): Bitmap {
    if (rotationDegrees == 0) return source

    val matrix = Matrix().apply {
        postRotate(rotationDegrees.toFloat())
    }

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

/**
 * Center-crops the source bitmap to match the target aspect ratio,
 * then resizes it to the ML-required resolution.
 *
 * This ensures consistent input size for the ML model:
 * - Square or rectangular crops
 * - Bilinear filtering for smooth resize
 */
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

    // Decide crop shape by comparing aspect ratios
    if (srcAspect > targetAspect) {
        // Too wide → trim width
        cropHeight = srcHeight
        cropWidth = (srcHeight * targetAspect).toInt()
        cropX = (srcWidth - cropWidth) / 2
        cropY = 0
    } else {
        // Too tall → trim height
        cropWidth = srcWidth
        cropHeight = (srcWidth / targetAspect).toInt()
        cropX = 0
        cropY = (srcHeight - cropHeight) / 2
    }

    // Perform crop
    val cropped = Bitmap.createBitmap(
        source,
        cropX.coerceAtLeast(0),
        cropY.coerceAtLeast(0),
        cropWidth.coerceAtMost(srcWidth),
        cropHeight.coerceAtMost(srcHeight)
    )

    // Resize to final ML input resolution
    return cropped.scale(
        width = targetWidth,
        height = targetHeight,
        filter = true // bilinear filtering for smoother result
    )
}