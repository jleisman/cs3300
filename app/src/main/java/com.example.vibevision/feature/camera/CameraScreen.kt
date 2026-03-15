package com.example.vibevision.feature.camera

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch

// TODO: Split this file into CameraScreen and CameraPermission Screens files

/**
 * High-level Camera screen.
 *
 * Responsibilities:
 * - Handle camera permission flow
 * - Route UI to: CameraPreview or deny screens
 * - (Next step): receive Bitmap from CameraPreview and save PNG or run ML
 */
@Composable
fun CameraScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val permission = Manifest.permission.CAMERA

    // Tracks whether the user currently has camera permission.
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, permission) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    // Tracks how many times the user denied the system permission dialog.
    var denialCount by remember { mutableIntStateOf(0) }

    /**
     * Permission launcher used when:
     * - The app launches for the first time
     * - The user taps "Grant Permission" on FirstDenyScreen
     */
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            denialCount += 1 // increment denial count
        }
        hasPermission = granted
    }

    /**
     * On first launch, automatically request permission.
     * (Only happens once because denialCount starts at 0.)
     */
    LaunchedEffect(Unit) {
        if (!hasPermission && denialCount == 0) {
            launcher.launch(permission)
        }
    }

    /**
     * When the app returns from background → foreground, re-check permission.
     *
     * This covers the case where:
     * - User denied twice
     * - App sent them to Settings
     * - They manually granted permission
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
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // ----------- MAIN UI ROUTING ----------- //

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when {
            // CASE 1: permission granted → show actual camera feed
            hasPermission -> {

                val saveScope = rememberCoroutineScope()

                CameraPreview(
                    modifier = Modifier.fillMaxSize(),
                    cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA,
                    onFrameCaptured = { bitmap ->
                        // Must run on background thread → we use coroutineScope
                        saveScope.launch {
                            val file = saveBitmapAsPng(context, bitmap)
                            println("Saved PNG to: ${file.absolutePath}")

                        }
                    }
                )
            }

            // CASE 2: permission denied once → allow user to retry
            denialCount == 1 -> {
                FirstDenyScreen(
                    onRequestPermission = { launcher.launch(permission) }
                )
            }

            // CASE 3: denied twice or more → direct user to system Settings
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


/**
 * Screen shown after user's first permission denial.
 */
@Composable
fun FirstDenyScreen(onRequestPermission: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Camera permission is required to use this app.")
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRequestPermission) {
            Text("Grant camera permission")
        }
    }
}

/**
 * Screen shown after second denial.
 * User must manually enable permission from Android Settings.
 */
@Composable
fun PermanentDenyScreen(onOpenSettings: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Camera permission has been denied.\n" +
                    "This app requires camera permissions.\n" +
                    "Enable it in Android settings."
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onOpenSettings) {
            Text("Open App Settings")
        }
    }
}
