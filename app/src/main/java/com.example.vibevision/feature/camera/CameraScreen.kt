package com.example.vibevision.feature.camera

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel


/*
 * Top-level screen responsible for:
 * - Handling camera permission flow
 * - Directing UI to camera or permission screens
 * - Passing captured frames to the ViewModel
 */
@Composable
fun CameraScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val permission = Manifest.permission.CAMERA

    // Injected ViewModel used to process each captured frame
    val viewModel: CameraViewModel = hiltViewModel()

    // Whether the user currently has camera permission
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, permission) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    // Tracks how many times the user denied the permission dialog
    var denialCount by remember { mutableIntStateOf(0) }

    // Launcher for system permission dialog
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            // Helps determine when to show "Go to Settings"
            denialCount += 1
        }
        hasPermission = granted
    }

    // Request camera permission on first appearance
    LaunchedEffect(Unit) {
        if (!hasPermission && denialCount == 0) {
            launcher.launch(permission)
        }
    }

    /*
     * Re-check permission whenever the Activity resumes.
     * Handles the scenario where the user enabled permission in Settings.
     */
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = ContextCompat.checkSelfPermission(
                    context,
                    permission
                ) == PackageManager.PERMISSION_GRANTED
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Permission-gated UI
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when {
            // CASE 1 — Permission granted → show camera preview
            hasPermission -> {
                CameraPreview(
                    modifier = Modifier.fillMaxSize(),
                    onFrameCaptured = { bitmap ->
                        viewModel.onBitmapCaptured(bitmap)
                    }
                )
            }

            // CASE 2 — First denial → allow user to retry
            denialCount == 1 -> {
                FirstDenyScreen(
                    onRequestPermission = { launcher.launch(permission) }
                )
            }

            // CASE 3 — Multiple denials → direct user to Settings
            denialCount >= 2 -> {
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