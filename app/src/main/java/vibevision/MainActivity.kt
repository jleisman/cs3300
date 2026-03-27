package com.vibevision

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.core.content.edit
import com.vibevision.feature.camera.CameraScreen
import com.vibevision.onboarding.OnboardingScreen
import com.vibevision.ui.theme.VibeVisionTheme
import dagger.hilt.android.AndroidEntryPoint

/*
 * Single-Activity entry point. A single `screen` state variable
 * drives which composable is visible — no NavHost required.
 *
 * Screen flow:
 *   Onboarding (first launch only) - Camera
 *
 * SharedPreferences persists whether onboarding has been completed
 * so it is skipped on every subsequent launch.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Read before setContent so the initial screen state is correct on first frame
        val prefs = getPreferences(MODE_PRIVATE)
        val hasSeenOnboarding = prefs.getBoolean("onboarding_done", false)

        setContent {
            VibeVisionTheme {
                Surface {
                    var screen by remember {
                        mutableStateOf(
                            if (hasSeenOnboarding) Screen.Camera else Screen.Onboarding
                        )
                    }

                    // Carries the permission result from OnboardingScreen into CameraScreen
                    // so CameraScreen does not need to re-query PackageManager on first render
                    var permissionGranted by remember { mutableStateOf(false) }

                    when (screen) {
                        Screen.Onboarding -> {
                            OnboardingScreen(
                                onPermissionResult = { granted ->
                                    prefs.edit { putBoolean("onboarding_done", true) }
                                    permissionGranted = granted
                                    screen = Screen.Camera
                                }
                            )
                        }
                        Screen.Camera -> {
                            CameraScreen(initialGranted = permissionGranted)
                        }
                    }
                }
            }
        }
    }
}

private enum class Screen { Onboarding, Camera }