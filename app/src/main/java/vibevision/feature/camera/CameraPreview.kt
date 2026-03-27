package com.vibevision.feature.camera

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

/*
 * Displays a CameraX PreviewView inside a Compose layout and
 * captures frames at a fixed interval for ML processing.
 *
 * Responsibilities:
 * - Host CameraX Preview inside Compose using AndroidView
 * - Configure ImageAnalysis and throttle frame capture every N ms
 * - Convert ImageProxy to Bitmap, rotate, crop, and resize
 * - Deliver a standardized Bitmap to the ViewModel via onFrameCaptured
 */
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    cameraSelector: CameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA,
    analysisIntervalMillis: Long = 5_000L,  // Minimum ms between frames sent to ML
    targetWidth: Int = 128,                 // ML model input width
    targetHeight: Int = 128,                // ML model input height
    onFrameCaptured: (Bitmap) -> Unit       // Delivers each processed frame to the caller
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Retrieving a CameraProvider is expensive — remember it so it survives recompositions
    val cameraProviderFuture = remember {
        ProcessCameraProvider.getInstance(context)
    }

    // A single-thread executor guarantees frames are analyzed one at a time,
    // preventing concurrent access to the YUV converter and bitmap operations
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    // Kept in remember so the converter is not re-allocated on every recomposition
    val yuvToRgbConverter = remember { YuvToRgbConverter() }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                // FIT_CENTER keeps the aspect ratio intact instead of stretching to fill
                scaleType = PreviewView.ScaleType.FIT_CENTER
            }

            val previewUseCase = Preview.Builder()
                .build()
                .also { preview ->
                    preview.surfaceProvider = previewView.surfaceProvider
                }

            // Tracks the timestamp of the last frame that was processed.
            // AtomicLong is used because the analyzer runs on a background thread
            val lastAnalyzedTimestamp = AtomicLong(0L)

            val analysisUseCase = ImageAnalysis.Builder()
                // STRATEGY_KEEP_ONLY_LATEST drops queued frames so the analyzer
                // always works on the most recent frame rather than a backlog
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                        try {
                            val now = System.currentTimeMillis()
                            val last = lastAnalyzedTimestamp.get()

                            // Skip this frame if not enough time has passed since the last one.
                            // This throttles ML calls without blocking the camera pipeline
                            if (now - last >= analysisIntervalMillis) {
                                lastAnalyzedTimestamp.set(now)

                                val rotationDegrees = imageProxy.imageInfo.rotationDegrees

                                val bitmap = imageProxy.toBitmap(yuvToRgbConverter)

                                // CameraX reports rotation separately from pixel data —
                                // apply it here so the bitmap is always upright before ML
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
                            // imageProxy must always be closed — failing to do so causes
                            // CameraX to stall and stop delivering new frames
                            imageProxy.close()
                        }
                    }
                }

            // addListener fires on the main executor once the CameraProvider is ready.
            // unbindAll before rebinding ensures no use case from a previous
            // configuration (e.g. rotation) remains attached
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

/*
 * Rotates a bitmap by the degrees reported by CameraX.
 *
 * CameraX delivers raw sensor pixels without applying device orientation,
 * so rotation must be applied manually before passing the bitmap to the ML model.
 * Returns the original bitmap unchanged if rotation is 0 to avoid an unnecessary copy.
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

/*
 * Center-crops the source bitmap to match the target aspect ratio,
 * then resizes it to the exact dimensions required by the ML model.
 *
 * Cropping before resizing avoids distortion — the subject stays proportional
 * regardless of what aspect ratio the camera delivered.
 * coerceAtLeast and coerceAtMost guard against off-by-one errors at the bitmap edges.
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

    // Compare aspect ratios to decide which dimension to trim.
    // The goal is the largest centered rectangle that fits the target ratio
    if (srcAspect > targetAspect) {
        // Source is wider than needed — trim the left and right sides equally
        cropHeight = srcHeight
        cropWidth = (srcHeight * targetAspect).toInt()
        cropX = (srcWidth - cropWidth) / 2
        cropY = 0
    } else {
        // Source is taller than needed — trim the top and bottom equally
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

    // filter = true applies bilinear filtering during the scale operation,
    // which produces smoother results than nearest-neighbor at the cost of
    // a small amount of extra CPU time
    return cropped.scale(
        width = targetWidth,
        height = targetHeight,
        filter = true
    )
}