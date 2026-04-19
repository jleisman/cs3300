package com.vibevision.feature.camera

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme.colorScheme
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
        Text(
            text = "Camera permission is required to use this app.",
            color = colorScheme.secondary)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRequestPermission,
            colors = ButtonDefaults.buttonColors(
                containerColor = colorScheme.tertiary,
                contentColor = colorScheme.onTertiary
            )
        ) {
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
            text = "Camera permission has been denied.\n" +
                    "This app requires camera access.\n" +
                    "Enable it in Android settings.",
            color = colorScheme.secondary
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onOpenSettings,
            colors = ButtonDefaults.buttonColors(
            containerColor = colorScheme.tertiary,
            contentColor = colorScheme.onTertiary
            )
        ) {
            Text("Open App Settings")
        }
    }
}
