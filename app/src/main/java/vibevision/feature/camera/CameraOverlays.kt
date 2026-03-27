package com.vibevision.feature.camera

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

/*
 * Composables and helpers that render on top of the camera preview.
 *
 * EmotionOverlay  - displays the current emotion label and confidence score
 * FaceAlignmentBox - draws a colored border to guide face positioning
 * colorForEmotion  - maps an emotion string to its corresponding border color
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
            text = "Confidence: ${"%.1f".format(confidence)}%",
            color = Color(0xFF00FF00),
            fontFamily = FontFamily.Monospace
        )
    }
}

/*
 * Draws a rounded border box centred on the preview.
 * Used to show the user where to position their face.
 * The color is driven by colorForEmotion so it reacts to the current ML result.
 * The size and position are controlled by the caller via modifier.
 */
@Composable
fun FaceAlignmentBox(
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .border(
                width = 2.dp,
                color = color,
                // Rounded corners soften the box so it reads as a guide rather than a hard crop
                shape = RoundedCornerShape(8.dp)
            )
    )
}

/*
 * Maps an emotion label from the ML model to a display color for FaceAlignmentBox.
 * Colors are chosen to be intuitive and distinct from each other at a glance.
 * Any unrecognized label defaults to white so the box remains visible.
 */
fun colorForEmotion(emotion: String): Color {
    return when (emotion.lowercase()) {
        "happy"   -> Color(0xFFFFFF00)  // yellow
        "angry"   -> Color(0xFFFF0000)  // red
        "sad"     -> Color(0xFF2196F3)  // blue
        "neutral" -> Color(0xFF00FF00)  // green
        else      -> Color.White
    }
}