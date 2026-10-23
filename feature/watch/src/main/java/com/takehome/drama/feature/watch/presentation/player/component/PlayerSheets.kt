package com.takehome.drama.feature.watch.presentation.player.component

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.takehome.drama.core.designsystem.component.ChoiceChip
import com.takehome.drama.core.designsystem.icon.AppIcons
import com.takehome.drama.core.designsystem.theme.AppTheme
import com.takehome.drama.core.designsystem.theme.AppTypography
import com.takehome.drama.domain.model.Quality
import com.takehome.drama.domain.usecase.watch.EpisodeCell
import com.takehome.drama.domain.usecase.watch.EpisodeRange
import com.takehome.drama.feature.watch.R
import com.takehome.drama.feature.watch.presentation.common.EpisodeGridCell
import com.takehome.drama.feature.watch.presentation.player.model.PlaybackSpeed
import java.util.Locale

private const val GRID_COLUMNS = 5
private const val HD_HEIGHT = 1080

/** Nội dung SCR-0303 (đặt trong `AppBottomSheet`): tab chunk 20 tập, lưới 5 cột, ô trên nền `bg`. */
@Composable
internal fun EpisodePickerSheet(
    cells: List<EpisodeCell>,
    ranges: List<EpisodeRange>,
    selectedRange: EpisodeRange?,
    watchedCount: Int,
    onRangeSelect: (EpisodeRange) -> Unit,
    onEpisodeSelect: (Int) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Text(
            text = stringResource(R.string.watch_episodes),
            style = AppTypography.title.copy(fontSize = 18.sp),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(
                R.string.watch_episodes_summary,
                pluralStringResource(R.plurals.watch_episode_count, cells.size, cells.size),
                watchedCount,
            ),
            style = AppTypography.label,
            color = AppTheme.colors.text3,
        )
    }
    if (ranges.size > 1) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.dimensions.s2),
        ) {
            ranges.forEach { range ->
                ChoiceChip(
                    text = stringResource(R.string.watch_range, range.firstLabel, range.lastLabel),
                    selected = range == selectedRange,
                    onClick = { onRangeSelect(range) },
                )
            }
        }
    }
    Spacer(Modifier.height(AppTheme.dimensions.s3))
    val range = selectedRange ?: ranges.firstOrNull()
    val visible = if (range == null) cells else cells.subList(range.start.coerceAtMost(cells.size), range.endExclusive.coerceAtMost(cells.size))
    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.dimensions.s2)) {
        visible.chunked(GRID_COLUMNS).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.dimensions.s2)) {
                row.forEach { cell ->
                    EpisodeGridCell(cell = cell, onClick = onEpisodeSelect, onSheet = true, modifier = Modifier.weight(1f))
                }
                repeat(GRID_COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** Nội dung SCR-0304 (đặt trong `AppBottomSheet`): tốc độ → quality có thật trong `sources` → ngôn ngữ → autoplay. */
@Composable
internal fun PlaybackOptionsSheet(
    speed: Float,
    qualities: List<String>,
    quality: String?,
    languages: List<String>,
    language: String,
    autoplayNext: Boolean,
    onSpeedSelect: (Float) -> Unit,
    onQualitySelect: (String?) -> Unit,
    onLanguageSelect: (String) -> Unit,
    onAutoplayToggle: () -> Unit,
) {
    Text(
        text = stringResource(R.string.watch_playback_options),
        style = AppTypography.title.copy(fontSize = 18.sp),
        color = MaterialTheme.colorScheme.onSurface,
    )
    GroupLabel(stringResource(R.string.watch_speed), Modifier.padding(top = 18.dp))
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.dimensions.s2),
    ) {
        PlaybackSpeed.options.forEach { option ->
            ChoiceChip(
                text = stringResource(R.string.watch_speed_value, PlaybackSpeed.label(option)),
                selected = option == speed,
                onClick = { onSpeedSelect(option) },
            )
        }
    }

    GroupLabel(stringResource(R.string.watch_quality), Modifier.padding(top = AppTheme.dimensions.s5))
    OptionRow(
        title = stringResource(R.string.watch_quality_auto),
        subtitle = stringResource(R.string.watch_quality_auto_hint),
        selected = quality == null,
        onClick = { onQualitySelect(null) },
    )
    qualities.forEach { option ->
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        OptionRow(
            title = option,
            subtitle = if ((Quality.heightOf(option) ?: 0) >= HD_HEIGHT) stringResource(R.string.watch_quality_more_data) else null,
            selected = quality == option,
            onClick = { onQualitySelect(option) },
        )
    }

    if (languages.size > 1) {
        GroupLabel(stringResource(R.string.watch_language), Modifier.padding(top = AppTheme.dimensions.s5))
        languages.forEachIndexed { i, option ->
            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            OptionRow(
                title = endonym(option),
                subtitle = null,
                selected = option == language,
                onClick = { onLanguageSelect(option) },
            )
        }
    }

    Row(
        modifier = Modifier
            .padding(top = AppTheme.dimensions.s3)
            .fillMaxWidth()
            .defaultMinSize(minHeight = AppTheme.dimensions.touchTarget)
            .toggleable(value = autoplayNext, role = Role.Switch, onValueChange = { onAutoplayToggle() })
            .padding(vertical = AppTheme.dimensions.s3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.dimensions.s3),
    ) {
        Icon(AppIcons.Volume, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Text(
            text = stringResource(R.string.watch_autoplay_next),
            style = AppTypography.body.copy(fontSize = 14.5.sp),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = autoplayNext,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                checkedThumbColor = AppTheme.colors.media.text,
                checkedBorderColor = MaterialTheme.colorScheme.primary,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                uncheckedThumbColor = AppTheme.colors.text3,
                uncheckedBorderColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            ),
        )
    }
}

@Composable
private fun GroupLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = AppTypography.micro.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp),
        color = AppTheme.colors.text3,
        modifier = modifier.padding(bottom = AppTheme.dimensions.s2),
    )
}

@Composable
private fun OptionRow(title: String, subtitle: String?, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = AppTheme.dimensions.touchTarget)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = AppTypography.body.copy(fontSize = 14.5.sp), color = MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = AppTypography.caption.copy(fontSize = 11.5.sp),
                    color = AppTheme.colors.text3,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }
        if (selected) {
            Icon(AppIcons.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        } else {
            Spacer(Modifier.width(AppTheme.dimensions.s3))
        }
    }
}

/** Tên ngôn ngữ bằng chính ngôn ngữ đó ("العربية", "हिन्दी") — người đọc nó tự nhận ra. */
private fun endonym(code: String): String {
    val locale = Locale.forLanguageTag(code)
    val name = locale.getDisplayLanguage(locale).ifBlank { code }
    return name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
}
