package com.example.vibevision

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.example.vibevision.feature.camera.CameraScreen


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface {
                    CameraScreen(
                        onCaptureClick = {
                            // Later: hook this up to ImageCapture to take a photo
                        }
                    )
                }
            }
        }
    }
}
