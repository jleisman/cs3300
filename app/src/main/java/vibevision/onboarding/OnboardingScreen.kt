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
import kotlinx.coroutines.launch

/*
 * Two-page onboarding flow shown on first launch.
 *
 * Page 0 - Welcome and app description
 * Page 1 - Privacy explainer and camera permission request
 *
 * Calls onPermissionResult with the system dialog result.
 * The caller is responsible for deciding what to show next.
 */

// Defined once here so both rememberPagerState and PageIndicator stay in sync
private const val PAGE_COUNT = 2

@Composable
fun OnboardingScreen(
    onPermissionResult: (granted: Boolean) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })

    // Required to call pagerState.animateScrollToPage from inside a click handler,
    // since animateScrollToPage is a suspend function
    val scope = rememberCoroutineScope()

    // Launches the system camera permission dialog. The result is forwarded
    // directly to the caller via onPermissionResult — this screen does not
    // decide what happens next
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        onPermissionResult(granted)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
    ) {
        // HorizontalPager renders one page at a time and supports swipe gestures.
        // Each page index maps to a composable in the 'when' block below
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> WelcomePage(
                    onNext = {
                        // Animate scroll rather than snap for a smoother page transition
                        scope.launch { pagerState.animateScrollToPage(1) }
                    }
                )
                1 -> PrivacyPage(
                    onEnableCamera = {
                        launcher.launch(Manifest.permission.CAMERA)
                    }
                )
            }
        }

        // Overlaid on top of the pager, pinned to the bottom of the screen
        PageIndicator(
            pageCount = PAGE_COUNT,
            currentPage = pagerState.currentPage,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        )
    }
}

// Page 0 - Simple welcome screen with text

@Composable
private fun WelcomePage(onNext: () -> Unit) {
    // Center everything vertically so the content sits in the middle of the screen
    // regardless of device height. Horizontal padding keeps text away from screen edges
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App title split across two lines to create a bold visual anchor at the top
        // of the page. Acts as a logo placeholder until a graphic asset is added.
        // 48sp is large enough to dominate without overflowing on small screens.
        // lineHeight = 52sp (just above fontSize) tightens the two lines together
        Text(
            text = "VIBE\nVISION",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 48.sp,
            color = Color(0xFF00FF00),
            textAlign = TextAlign.Center,
            lineHeight = 52.sp
        )

        // Larger gap here than between the two body texts below — separates the
        // title visually from the supporting copy
        Spacer(Modifier.height(32.dp))

        // Primary tagline — single sentence, brighter color (0xFFCCCCCC) so it
        // reads as the first thing after the title
        Text(
            text = "Real-time emotion detection from your face.",
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            color = Color(0xFFCCCCCC),
            textAlign = TextAlign.Center
        )

        // Tighter gap here — the two body texts are related so they sit closer together
        Spacer(Modifier.height(12.dp))

        // Secondary description at a smaller size and dimmer color (0xFF888888) to
        // reinforce the hierarchy: title, then tagline, then supporting detail.
        // lineHeight = 20sp gives the wrapped text enough breathing room to stay readable
        Text(
            text = "Point your front camera at yourself and watch " +
                    "the app classify your mood live — no account, " +
                    "no cloud, no data leaving your device.",
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            color = Color(0xFF888888),
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        // Larger gap before the button creates clear separation between content and CTA
        Spacer(Modifier.height(48.dp))

        OnboardingButton(label = "Get Started", onClick = onNext)
    }
}

// Page 1 - Privacy Page explaining why camera permission is needed

@Composable
private fun PrivacyPage(onEnableCamera: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Section heading at 24sp — smaller than the welcome title but still prominent.
        // Same green as the title on page 0 keeps the two pages visually consistent
        Text(
            text = "Before we start",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = Color(0xFF00FF00)
        )

        // Gap between heading and first card — slightly smaller than the 32dp gap on
        // page 0 because the cards themselves provide visual weight below
        Spacer(Modifier.height(28.dp))

        // Three cards covering camera use, data handling, and account policy.
        // Order is intentional: what the app needs, what it does with it, what it does not do.
        // Shown before the permission dialog so the user understands the reason
        // for the request before Android displays it
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
                    "No images, no emotion data, nothing is ever uploaded or shared."
        )

        Spacer(Modifier.height(12.dp))

        PrivacyCard(
            icon = "🚫",
            title = "No accounts. No tracking.",
            body = "There is no sign-in, no analytics SDK, and no third-party data collection."
        )

        // Larger gap before the CTA — more space here than between cards to signal
        // that the button is a distinct action rather than part of the card list
        Spacer(Modifier.height(40.dp))

        // Tapping this triggers the system permission dialog.
        // Android will only show the dialog if permission has not already been
        // granted or permanently denied
        OnboardingButton(label = "Enable Camera", onClick = onEnableCamera)

        Spacer(Modifier.height(12.dp))

        // Small, dimmed text below the button — reassures hesitant users that
        // they are not making a permanent decision by tapping Enable Camera
        Text(
            text = "You can change this later in Android Settings.",
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = Color(0xFF555555),
            textAlign = TextAlign.Center
        )
    }
}

// Shared composables

@Composable
private fun PrivacyCard(icon: String, title: String, body: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1A1A1A))
            .padding(16.dp),
        // Top-aligned so the icon sits next to the title rather than centering
        // vertically when the body text wraps to multiple lines
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

@Composable
private fun OnboardingButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(12.dp),
        // Green on near-black matches the app's monochrome palette
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF00FF00),
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

@Composable
private fun PageIndicator(pageCount: Int, currentPage: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            // Animate color when the active page changes rather than snapping instantly
            val color by animateColorAsState(
                targetValue = if (index == currentPage) Color(0xFF00FF00) else Color(0xFF333333),
                animationSpec = tween(300),
                label = "dot_color"
            )
            // Active dot is slightly larger to reinforce which page is current
            Box(
                modifier = Modifier
                    .size(if (index == currentPage) 10.dp else 7.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}