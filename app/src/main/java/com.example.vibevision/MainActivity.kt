package com.example.vibevision

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.example.vibevision.feature.camera.CameraScreen
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            // If you have your own theme, wrap it here instead of MaterialTheme
            MaterialTheme {
                Surface {
                    // CameraScreen manages:
                    // - Permission logic (including "Don't ask again" handling)
                    // - Showing the CameraPreview when permission is granted
                    // - Showing the proper fallback UI when not granted
                    CameraScreen()
                }
            }
        }
    }
}
