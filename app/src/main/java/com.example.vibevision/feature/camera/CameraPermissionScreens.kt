package com.example.vibevision.feature.camera

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/*
 * These screens handle the UI states when permission is denied:
 * - FirstDenyScreen: user can retry the permission request.
 * - PermanentDenyScreen: user must open Android Settings manually.
 */

/**
 * Shown after the user denies permission once.
 * Offers a retry using the system permission dialog.
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
 * Shown after the user denies permission multiple times.
 *
 * At this point Android will no longer show the permission dialog.
 * The user must manually enable permission in the system Settings UI.
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
                    "This app requires camera access.\n" +
                    "Enable it in Android settings."
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onOpenSettings) {
            Text("Open App Settings")
        }
    }
}
