package com.vibevision.feature.camera

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

/*
 * Top-level screen that reflects the current camera permission state.
 * The initial permission request is handled by OnboardingScreen, not here.
 *
 * Permission states:
 *   granted          - show live camera preview with emotion overlay
 *   denialCount 0-1  - show FirstDenyScreen so the user can retry
 *   denialCount 2+   - show PermanentDenyScreen and direct user to Settings
 */
// Result passed in from OnboardingScreen to avoid a redundant permission check on first render
@Composable
fun CameraScreen(
    initialGranted: Boolean = false
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val permission = Manifest.permission.CAMERA

    val viewModel: CameraViewModel = hiltViewModel()

    // Use the value passed from OnboardingScreen, or fall back to a fresh permission check
    var hasPermission by remember {
        mutableStateOf(
            initialGranted ||
                    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Counts denials that occur inside CameraScreen only (via FirstDenyScreen retry).
    // Starts at 0 so the user always gets at least one retry attempt after onboarding.
    var denialCount by remember { mutableIntStateOf(0) }

    // Launcher used by FirstDenyScreen's retry button
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) denialCount += 1
        hasPermission = granted
    }

    /*
     * Re-check permission on every Activity resume.
     * Catches the case where the user grants permission in Android Settings
     * and returns to the app without relaunching it.
     */
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = ContextCompat.checkSelfPermission(
                    context, permission
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colorScheme.primary
    ) {
        when {
            // Permission granted - show live preview
            hasPermission -> {
                val emotion by viewModel.emotion.collectAsState()
                val confidence by viewModel.confidence.collectAsState()

                Box(modifier = Modifier.fillMaxSize()) {

                    CameraPreview(
                        modifier = Modifier.fillMaxSize(),
                        onFrameCaptured = { bitmap ->
                            viewModel.onBitmapCaptured(bitmap)
                        }
                    )

                    EmotionOverlay(
                        emotion = emotion,
                        confidence = confidence,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 16.dp, top = 96.dp)
                    )
                }
            }

            // User denied once - offer a retry via the system permission dialog
            denialCount == 0 || denialCount == 1 -> {
                FirstDenyScreen(
                    onRequestPermission = { launcher.launch(permission) }
                )
            }

            // User denied twice or more - Android will no longer show the dialog,
            // so direct them to enable permission manually in Settings
            else -> {
                PermanentDenyScreen(
                    onOpenSettings = {
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null)
                        )
                        context.startActivity(intent)
                    }
                )
            }
        }
    }
}