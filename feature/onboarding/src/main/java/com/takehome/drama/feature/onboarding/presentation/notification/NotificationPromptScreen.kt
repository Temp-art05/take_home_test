package com.takehome.drama.feature.onboarding.presentation.notification

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.takehome.drama.core.designsystem.theme.AppTheme
import com.takehome.drama.core.designsystem.theme.AppTypography
import com.takehome.drama.core.ui.foundation.format.format
import com.takehome.drama.core.ui.foundation.format.rememberTimeFormat
import com.takehome.drama.domain.usecase.discovery.Daypart
import com.takehome.drama.domain.usecase.discovery.opensAt
import com.takehome.drama.core.ui.foundation.ext.CollectAsEffect
import com.takehome.drama.feature.onboarding.R
import com.takehome.drama.feature.onboarding.presentation.component.OnboardingScaffold

@Composable
fun NotificationPromptScreen(
    viewModel: NotificationPromptViewModel,
    onFinish: () -> Unit,
) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.processIntent(NotificationPromptIntent.PermissionResult(granted))
    }
    viewModel.singleEvent.CollectAsEffect { event ->
        when (event) {
            NotificationPromptSingleEvent.RequestPermission ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    // Không tới được đây (0004 bỏ qua màn này dưới API 33) — vẫn đi tiếp cho chắc.
                    viewModel.processIntent(NotificationPromptIntent.PermissionResult(granted = true))
                }
            NotificationPromptSingleEvent.Finish -> onFinish()
        }
    }
    NotificationPromptContent(onIntent = viewModel::processIntent)
}

/** Khung giờ ping — trùng 3 daypart của Home Day Cycle (giờ lấy từ [opensAt]). */
private data class PingSlot(val emoji: String, @StringRes val nameRes: Int, @StringRes val bodyRes: Int, val daypart: Daypart)

private val slots = listOf(
    PingSlot("☕", R.string.onboarding_ping_morning, R.string.onboarding_ping_morning_body, Daypart.Morning),
    PingSlot("🌤", R.string.onboarding_ping_lunch, R.string.onboarding_ping_lunch_body, Daypart.Lunch),
    PingSlot("🌙", R.string.onboarding_ping_night, R.string.onboarding_ping_night_body, Daypart.Night),
)

@Composable
private fun NotificationPromptContent(onIntent: (NotificationPromptIntent) -> Unit) {
    OnboardingScaffold(
        step = 4,
        title = stringResource(R.string.onboarding_notification_title),
        body = stringResource(R.string.onboarding_notification_body),
        primaryLabel = stringResource(R.string.onboarding_turn_on_notifications),
        onPrimary = { onIntent(NotificationPromptIntent.TurnOn) },
        secondaryLabel = stringResource(R.string.onboarding_not_now),
        onSecondary = { onIntent(NotificationPromptIntent.NotNow) },
    ) {
        val timeFormat = rememberTimeFormat()
        Column(Modifier.padding(top = AppTheme.dimensions.s5), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            slots.forEach { slot ->
                val time = remember(timeFormat, slot) { slot.daypart.opensAt.format(timeFormat) }
                SlotCard(slot = slot, time = time)
            }
        }
    }
}

@Composable
private fun SlotCard(slot: PingSlot, time: String) {
    val colors = MaterialTheme.colorScheme
    val shape = AppTheme.shapes.card
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface, shape)
            .border(1.dp, colors.outline, shape)
            .padding(horizontal = 14.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.dimensions.s3),
    ) {
        Text(text = slot.emoji, style = AppTypography.title.copy(fontSize = 20.sp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppTheme.dimensions.s1)) {
            Text(
                text = stringResource(R.string.onboarding_ping_title, stringResource(slot.nameRes), time),
                style = AppTypography.label.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                color = colors.onSurface,
            )
            Text(
                text = stringResource(slot.bodyRes),
                style = AppTypography.caption.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                color = AppTheme.colors.text3,
            )
        }
    }
}

@Preview
@Composable
private fun NotificationPromptPreview() {
    AppTheme { NotificationPromptContent(onIntent = {}) }
}
