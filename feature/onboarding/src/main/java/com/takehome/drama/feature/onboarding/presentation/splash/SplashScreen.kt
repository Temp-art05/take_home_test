package com.takehome.drama.feature.onboarding.presentation.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.takehome.drama.core.designsystem.component.FlowMark
import com.takehome.drama.core.designsystem.component.Wordmark
import com.takehome.drama.core.designsystem.theme.AppTheme
import com.takehome.drama.core.designsystem.theme.AppTypography
import com.takehome.drama.core.navigation.core.NavigationRequest
import com.takehome.drama.core.navigation.destination.Destination
import com.takehome.drama.core.ui.foundation.ext.CollectAsEffect
import com.takehome.drama.feature.onboarding.R
import com.takehome.drama.feature.onboarding.navigation.OnboardingScreenProvider

@Composable
fun SplashScreen(
    viewModel: SplashViewModel,
    home: Destination,
    onNavigate: (NavigationRequest) -> Unit,
) {
    viewModel.singleEvent.CollectAsEffect { event ->
        val destination = when (event) {
            SplashSingleEvent.GoHome -> home
            SplashSingleEvent.GoOnboarding -> OnboardingScreenProvider.Onboarding()
        }
        onNavigate(NavigationRequest.NavigateToAndClearBackStack(destination))
    }
    SplashContent()
}

/** `.splash`: glow cam 14% sau logo, mark 78dp, wordmark 30sp, tag `text3`. Không progress bar. */
@Composable
private fun SplashContent() {
    val glow = AppTheme.colors.splashGlow
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .drawBehind {
                // radial-gradient(70% 50% at 50% 42%, glow, transparent 70%)
                drawRect(
                    Brush.radialGradient(
                        0f to glow,
                        0.7f to Color.Transparent,
                        center = Offset(size.width / 2, size.height * 0.42f),
                        radius = size.width * 0.7f,
                    ),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            FlowMark(size = AppTheme.dimensions.splashMark)
            Wordmark(splash = true, modifier = Modifier.padding(top = AppTheme.dimensions.s5))
            Text(
                text = stringResource(R.string.onboarding_splash_tag).uppercase(),
                style = AppTypography.caption.copy(fontSize = 12.sp, letterSpacing = 0.16.em, fontWeight = FontWeight.SemiBold),
                color = AppTheme.colors.text3,
                modifier = Modifier
                    .padding(top = AppTheme.dimensions.s3)
                    // Mockup `.tag`: pill viền 1dp; padding rộng hơn CSS 4/9 theo ảnh (≈6/18).
                    .border(1.dp, MaterialTheme.colorScheme.outline, AppTheme.shapes.full)
                    .padding(horizontal = 18.dp, vertical = 6.dp),
            )
        }
    }
}

@Preview
@Composable
private fun SplashPreview() {
    AppTheme { SplashContent() }
}
