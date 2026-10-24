package com.takehome.drama.feature.onboarding.presentation.taste

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.takehome.drama.core.designsystem.component.InlineErrorState
import com.takehome.drama.core.designsystem.ext.clickableNoRipple
import com.takehome.drama.core.designsystem.theme.AppTheme
import com.takehome.drama.core.designsystem.theme.AppTypography
import com.takehome.drama.core.ui.foundation.error.ErrorKind
import com.takehome.drama.core.ui.foundation.error.body
import com.takehome.drama.core.ui.foundation.error.retryLabel
import com.takehome.drama.core.ui.foundation.error.title
import com.takehome.drama.core.ui.foundation.ext.CollectAsEffect
import com.takehome.drama.domain.usecase.onboarding.Mood
import com.takehome.drama.feature.onboarding.R
import com.takehome.drama.feature.onboarding.presentation.component.OnboardingScaffold

@Composable
fun TastePickerScreen(
    viewModel: TastePickerViewModel,
    onNext: () -> Unit,
    onFinish: () -> Unit,
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    viewModel.singleEvent.CollectAsEffect { event ->
        when (event) {
            is TastePickerSingleEvent.Next -> if (event.showNotificationPrompt) onNext() else onFinish()
        }
    }
    // Chỉ đọc trạng thái quyền (không hỏi): quyết định có bước SCR-0005 nằm ở VM.
    val granted = {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }
    TastePickerContent(
        viewState = viewState,
        onIntent = viewModel::processIntent,
        onContinue = { viewModel.processIntent(TastePickerIntent.Continue(Build.VERSION.SDK_INT, granted())) },
        onSkip = { viewModel.processIntent(TastePickerIntent.Skip(Build.VERSION.SDK_INT, granted())) },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TastePickerContent(
    viewState: TastePickerViewState,
    onIntent: (TastePickerIntent) -> Unit,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
) {
    OnboardingScaffold(
        step = 3,
        title = stringResource(R.string.onboarding_taste_title),
        body = stringResource(R.string.onboarding_taste_body),
        primaryLabel = stringResource(R.string.onboarding_continue),
        onPrimary = onContinue,
        primaryEnabled = viewState.canContinue,
        secondaryLabel = stringResource(R.string.onboarding_skip),
        onSecondary = onSkip,
    ) {
        viewState.error?.let { error ->
            InlineErrorState(
                title = error.title(),
                body = error.body(),
                retryLabel = retryLabel(),
                onRetry = { onIntent(TastePickerIntent.Retry) },
                offline = error.kind == ErrorKind.Offline,
                modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
            )
            return@OnboardingScaffold
        }
        Column {
            // Lưới 2×2 thẻ mood.
            viewState.moods.chunked(2).forEachIndexed { row, pair ->
                Row(
                    modifier = Modifier.padding(top = if (row == 0) AppTheme.dimensions.s5 else 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    pair.forEach { item ->
                        MoodCard(
                            item = item,
                            selected = item.mood in viewState.selectedMoods,
                            onClick = { onIntent(TastePickerIntent.ToggleMood(item.mood)) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            if (viewState.chips.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.onboarding_fine_tune).uppercase(),
                    style = AppTypography.caption.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.04.em),
                    color = AppTheme.colors.text3,
                    modifier = Modifier.padding(top = 18.dp),
                )
                FlowRow(
                    modifier = Modifier.padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    viewState.visibleChips.forEach { chip ->
                        TasteChipView(
                            label = chip.label,
                            selected = chip.id in viewState.selectedTags,
                            onClick = { onIntent(TastePickerIntent.ToggleTag(chip.id)) },
                        )
                    }
                    if (viewState.hiddenChipCount > 0) {
                        TasteChipView(
                            label = stringResource(R.string.onboarding_more_chips, viewState.hiddenChipCount),
                            selected = false,
                            onClick = { onIntent(TastePickerIntent.ExpandChips) },
                            isToggle = false,
                        )
                    }
                }
            }
        }
    }
}

/** Thẻ mood: chọn = nền gradient + chữ `onAccent`; chưa chọn = `surface` + viền. */
@Composable
private fun MoodCard(item: MoodItem, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = AppTheme.shapes.lg
    val colors = MaterialTheme.colorScheme
    val content = if (selected) colors.onPrimary else colors.onBackground
    Column(
        modifier = modifier
            .clip(shape)
            .then(
                if (selected) {
                    Modifier.background(AppTheme.colors.gradientCta, shape)
                } else {
                    Modifier.background(colors.surface, shape).border(1.dp, colors.outline, shape)
                },
            )
            .toggleable(value = selected, role = Role.Checkbox, onValueChange = { onClick() })
            .padding(horizontal = AppTheme.dimensions.s3, vertical = 19.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = item.mood.emoji, style = AppTypography.title.copy(fontSize = 24.sp, lineHeight = 30.sp))
        Text(
            text = stringResource(item.mood.labelRes),
            style = AppTypography.bodySemibold.copy(fontWeight = FontWeight.ExtraBold),
            color = content,
            modifier = Modifier.padding(top = 5.dp),
        )
        Text(
            text = item.tagNames.joinToString(" · ").lowercase(),
            style = AppTypography.caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
            color = if (selected) content.copy(alpha = 0.75f) else AppTheme.colors.text3,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 1.dp),
        )
    }
}

/**
 * `.g` cỡ nhỏ của fine-tune (12.5sp, padding 8/13): bật = nền accent, chữ Bold.
 * Bề rộng luôn theo chữ Bold (bản ẩn giữ chỗ) — bật/tắt không làm chip phình ra và FlowRow không xếp lại hàng.
 */
@Composable
private fun TasteChipView(label: String, selected: Boolean, onClick: () -> Unit, isToggle: Boolean = true) {
    val colors = MaterialTheme.colorScheme
    val shape = AppTheme.shapes.full
    val style = AppTypography.label.copy(fontSize = 12.5.sp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(shape)
            .then(if (selected) Modifier.background(colors.primary, shape) else Modifier.border(1.dp, colors.outline, shape))
            // toggleable: TalkBack đọc "đã chọn / chưa chọn". Vùng chạm < 48dp được Compose tự nới khi hit-test.
            .then(
                if (isToggle) {
                    Modifier.toggleable(value = selected, interactionSource = null, indication = null, role = Role.Checkbox) { onClick() }
                } else {
                    Modifier.clickableNoRipple(role = Role.Button, onClick = onClick)
                },
            )
            .padding(horizontal = 13.dp, vertical = AppTheme.dimensions.s2),
    ) {
        Text(
            text = label,
            style = style.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            modifier = Modifier
                .alpha(0f)
                .clearAndSetSemantics {},
        )
        Text(
            text = label,
            style = style.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium),
            color = if (selected) colors.onPrimary else colors.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

private val Mood.emoji: String
    get() = when (this) {
        Mood.Rage -> "🔥"
        Mood.Swoon -> "😍"
        Mood.Cry -> "😭"
        Mood.Thrill -> "😱"
    }

@get:StringRes
private val Mood.labelRes: Int
    get() = when (this) {
        Mood.Rage -> R.string.onboarding_mood_rage
        Mood.Swoon -> R.string.onboarding_mood_swoon
        Mood.Cry -> R.string.onboarding_mood_cry
        Mood.Thrill -> R.string.onboarding_mood_thrill
    }

@Preview
@Composable
private fun TastePickerPreview() {
    AppTheme {
        TastePickerContent(
            viewState = TastePickerViewState(
                isLoading = false,
                moods = Mood.entries.map { MoodItem(it, listOf("Revenge", "Comeback")) },
                chips = listOf("Revenge", "Comeback", "Urban", "Fantasy", "CEO", "Love", "Comedy").map { TasteChip(it, it) },
                selectedMoods = setOf(Mood.Rage),
                selectedTags = setOf("Revenge", "Comeback"),
            ),
            onIntent = {},
            onContinue = {},
            onSkip = {},
        )
    }
}
