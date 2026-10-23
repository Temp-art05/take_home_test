package com.takehome.drama.feature.watch.presentation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.takehome.drama.core.designsystem.component.EpisodeCell
import com.takehome.drama.core.designsystem.component.EpisodeCellStyle
import com.takehome.drama.core.designsystem.component.GradientButton
import com.takehome.drama.core.designsystem.component.OutlinePillButton
import com.takehome.drama.core.designsystem.theme.AppTheme
import com.takehome.drama.core.designsystem.theme.AppTypography
import com.takehome.drama.feature.watch.R

/**
 * Nút chính mở tập bằng rewarded: gradient (đây là hành động chính của màn lúc đó). Đang load → spinner trong
 * nút và không bấm được; không có ad → mờ hẳn, caller hiện câu giải thích. Không chặn gì khác trên màn.
 */
@Composable
internal fun RewardedUnlockButton(
    text: String,
    state: RewardedAdState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val loadingDescription = stringResource(R.string.watch_unlock_loading_cd)
    GradientButton(
        text = text,
        onClick = onClick,
        enabled = state == RewardedAdState.Ready,
        modifier = modifier,
        leadingIcon = if (state == RewardedAdState.Loading) {
            {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                    modifier = Modifier
                        .size(16.dp)
                        .semantics { contentDescription = loadingDescription },
                )
            }
        } else {
            null
        },
    )
}

/** Nội dung sheet mở tập (đặt trong `AppBottomSheet`): ô tập khoá kiểu 0303, lời mời, nút ad, "Not now". */
@Composable
internal fun UnlockEpisodeSheetContent(
    episodeNumber: Int,
    ad: RewardedAdState,
    onWatchAd: () -> Unit,
    onNotNow: () -> Unit,
) {
    val dimensions = AppTheme.dimensions
    Row(verticalAlignment = Alignment.CenterVertically) {
        EpisodeCell(
            label = episodeNumber.toString(),
            style = EpisodeCellStyle.Locked,
            onClick = {},
            onSheet = true,
            lockedDescription = stringResource(R.string.watch_locked_cd),
            modifier = Modifier.width(dimensions.episodeCell),
        )
        Spacer(Modifier.width(dimensions.s4))
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.watch_unlock_title, episodeNumber),
                style = AppTypography.title,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(dimensions.s1))
            Text(
                text = stringResource(R.string.watch_unlock_body),
                style = AppTypography.body,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    if (ad == RewardedAdState.Unavailable) {
        Text(
            text = stringResource(R.string.watch_unlock_unavailable),
            style = AppTypography.label,
            color = AppTheme.colors.text3,
            modifier = Modifier.padding(top = dimensions.s4),
        )
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = dimensions.s5, bottom = dimensions.s2),
        verticalArrangement = Arrangement.spacedBy(dimensions.s3),
    ) {
        RewardedUnlockButton(
            text = stringResource(R.string.watch_unlock_watch_ad),
            state = ad,
            onClick = onWatchAd,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinePillButton(
            text = stringResource(R.string.watch_unlock_not_now),
            onClick = onNotNow,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview
@Composable
private fun UnlockEpisodeSheetPreview() {
    AppTheme {
        Column(
            Modifier
                .padding(16.dp)
                .fillMaxWidth(),
        ) {
            UnlockEpisodeSheetContent(episodeNumber = 6, ad = RewardedAdState.Loading, onWatchAd = {}, onNotNow = {})
        }
    }
}
