package com.vibevision.feature.camera

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

/*
 * Composables and helpers that render on top of the camera preview.
 *
 * EmotionOverlay  - displays the current emotion label and confidence score
 */

// Displays the ML output as a two-line text overlay in the top-left corner of the preview
@Composable
fun EmotionOverlay(
    emotion: String,
    confidence: Float,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Emotion: $emotion",
            color = Color(0xFF00FF00),
            fontFamily = FontFamily.Monospace
        )
        // "%.1f" formats confidence to one decimal place (e.g. 83.4%)
        Text(
            text = "Confidence: ${"%.2f".format(confidence * 100F)}%",
            color = Color(0xFF00FF00),
            fontFamily = FontFamily.Monospace
        )
    }
}