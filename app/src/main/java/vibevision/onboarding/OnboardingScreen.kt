package com.vibevision.onboarding

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibevision.ui.theme.DarkPrimary
import com.vibevision.ui.theme.HackerColor
import kotlinx.coroutines.launch

/* Joshua Leisman
 * Two-page onboarding flow shown on first app launch.
 *
 * Page 0: App introduction and value proposition
 * Page 1: Privacy explanation and camera permission request
 *
 * This screen does not decide navigation after onboarding.
 * It reports the camera permission result to the caller.
 */

// Total number of onboarding pages
private const val PAGE_COUNT = 2

@Composable
fun OnboardingScreen(
    onPermissionResult: (granted: Boolean) -> Unit
) {
    // Pager state shared between the pager and page indicator
    val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })

    // Coroutine scope required for animated page scrolling
    val scope = rememberCoroutineScope()

    /*
     * Activity result launcher for requesting camera permission.
     * The permission result is forwarded to the caller.
     */
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        onPermissionResult(granted)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkPrimary)
    ) {
        /*
         * Horizontal pager displaying onboarding pages.
         * Each page index maps to a specific composable.
         */
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> WelcomePage(
                    onNext = {
                        /* Animate transition to the next page */
                        scope.launch {
                            pagerState.animateScrollToPage(1)
                        }
                    }
                )
                1 -> PrivacyPage(
                    onEnableCamera = {
                        /* Trigger system camera permission dialog */
                        launcher.launch(Manifest.permission.CAMERA)
                    }
                )
            }
        }

        /*
         * Page indicator overlaid at the bottom of the screen.
         * Reflects the currently visible onboarding page.
         */
        PageIndicator(
            pageCount = PAGE_COUNT,
            currentPage = pagerState.currentPage,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        )
    }
}

// Page 0: Welcome and app description
@Composable
private fun WelcomePage(onNext: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App title displayed prominently
        Text(
            text = "VIBE\nVISION",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 48.sp,
            color = HackerColor,
            textAlign = TextAlign.Center,
            lineHeight = 52.sp
        )

        Spacer(Modifier.height(32.dp))

        // Primary tagline
        Text(
            text = "Real-time emotion detection from your face.",
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            color = colorScheme.secondary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(12.dp))

        // Supporting description explaining core behavior
        Text(
            text = "Point your front camera at yourself and watch " +
                    "the app classify your mood live — no account, " +
                    "no cloud, no data leaving your device.",
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            color = colorScheme.tertiary,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(Modifier.height(48.dp))

        // Button to advance to the privacy page
        OnboardingButton(
            label = "Get Started",
            onClick = onNext
        )
    }
}

// Page 1: Privacy explanation and permission request
@Composable
private fun PrivacyPage(onEnableCamera: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Section heading
        Text(
            text = "Before we start",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = HackerColor
        )

        Spacer(Modifier.height(28.dp))

        /*
         * Privacy explanation cards shown before requesting permission
         * to give context for the system dialog.
         */
        PrivacyCard(
            icon = "📷",
            title = "Camera access required",
            body = "VibeVision uses your front camera to read your facial expressions in real time."
        )

        Spacer(Modifier.height(12.dp))

        PrivacyCard(
            icon = "🔒",
            title = "Everything stays on-device",
            body = "Frames are processed locally and deleted immediately after analysis. " +
                    "No images or emotion data are uploaded or shared."
        )

        Spacer(Modifier.height(12.dp))

        PrivacyCard(
            icon = "🚫",
            title = "No accounts. No tracking.",
            body = "There is no sign-in, analytics SDK, or third-party data collection."
        )

        Spacer(Modifier.height(40.dp))

        // Button that triggers the camera permission dialog
        OnboardingButton(
            label = "Enable Camera",
            onClick = onEnableCamera
        )

        Spacer(Modifier.height(12.dp))

        // Reassurance text for users hesitant to grant permission
        Text(
            text = "You can change this later in Android Settings.",
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = Color(0xFF555555),
            textAlign = TextAlign.Center
        )
    }
}

// Reusable card used to present privacy information */
@Composable
private fun PrivacyCard(icon: String, title: String, body: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1A1A1A))
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(text = icon, fontSize = 20.sp)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = Color(0xFFEEEEEE)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = body,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = Color(0xFF888888),
                lineHeight = 18.sp
            )
        }
    }
}

// Reusable primary onboarding button */
@Composable
private fun OnboardingButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = HackerColor,
            contentColor = Color(0xFF0A0A0A)
        )
    ) {
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
    }
}

// Row of animated dots indicating the current onboarding page */
@Composable
private fun PageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            val color by animateColorAsState(
                targetValue = if (index == currentPage) HackerColor else Color(0xFF333333),
                animationSpec = tween(300),
                label = "dot_color"
            )

            Box(
                modifier = Modifier
                    .size(if (index == currentPage) 10.dp else 7.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}