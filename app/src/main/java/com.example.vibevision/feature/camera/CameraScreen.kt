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

@Composable
fun CameraScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val permission = Manifest.permission.CAMERA

    // If permission is currently granted
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, permission) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    // This tracks how many times the user has denied the request.
    var denialCount by remember { mutableIntStateOf(0) }

    // Permission launcher
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        granted ->
        // if camera permission denied, increment counter so correct screen is shown
        if (!granted) {
            denialCount += 1
        }
        hasPermission = granted
    }

    // Request permission automatically on first app launch
    LaunchedEffect(Unit) {
        if (!hasPermission && denialCount == 0) {
            launcher.launch(permission)
        }
    }

    // When returning from settings, re-check permission
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = ContextCompat.checkSelfPermission(
                    context,
                    permission
                ) == PackageManager.PERMISSION_GRANTED
            }
        }

        // for entire lifetime of app
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {

        when {
            // CASE 1 — Permission granted so show camera preview
            hasPermission -> {
                CameraPreview(
                    modifier = Modifier.fillMaxSize(),
                    cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                )
            }

            // CASE 2 — After first denial so show button to try again
            denialCount == 1 -> {
                FirstDenyScreen(
                    onRequestPermission = { launcher.launch(permission) }
                )
            }

            // CASE 3 — After second denial so open settings
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

// Screen shown after permissions dialog and allowed to ask permissions again
@Composable
fun FirstDenyScreen(onRequestPermission: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
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

// Screen shown after asking twice and being denied. Send user to settings page
@Composable
fun PermanentDenyScreen(onOpenSettings: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Camera permission has been denied.\n This apps requires camera permissions." +
                " \nEnable it in app settings.")
        Spacer(Modifier.height(16.dp))
        Button(onClick = onOpenSettings) {
            Text("Open app settings")
        }
    }
}