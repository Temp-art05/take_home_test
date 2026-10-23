package com.takehome.drama.feature.watch.presentation.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.takehome.drama.core.designsystem.component.ScreenSkeleton
import com.takehome.drama.core.designsystem.component.SkeletonAction
import com.takehome.drama.core.designsystem.theme.AppTheme
import com.takehome.drama.core.designsystem.theme.AppTypography
import com.takehome.drama.feature.watch.R

private enum class PlayerSheet { None, Episodes, PlaybackOptions }

/** Khung SCR-0302 + hai sheet SCR-0303 / SCR-0304. Phase 3 thay bằng player thật. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlayerSkeleton(
    showId: String,
    episodeIndex: Int,
    onBack: () -> Unit,
    onNextEpisode: () -> Unit,
) {
    var sheet by rememberSaveable { mutableStateOf(PlayerSheet.None) }

    ScreenSkeleton(
        code = "SCR-0302",
        title = stringResource(R.string.watch_player_title, showId, episodeIndex + 1),
        onBack = onBack,
        actions = listOf(
            SkeletonAction(stringResource(R.string.watch_next_episode), onNextEpisode),
            SkeletonAction(stringResource(R.string.watch_episodes)) { sheet = PlayerSheet.Episodes },
            SkeletonAction(stringResource(R.string.watch_playback_options)) { sheet = PlayerSheet.PlaybackOptions },
        ),
    )

    if (sheet != PlayerSheet.None) {
        ModalBottomSheet(
            onDismissRequest = { sheet = PlayerSheet.None },
            shape = AppTheme.shapes.sheet,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(AppTheme.dimensions.s6),
            ) {
                Text(
                    text = stringResource(
                        if (sheet == PlayerSheet.Episodes) {
                            R.string.watch_episode_picker_sheet
                        } else {
                            R.string.watch_playback_options_sheet
                        }
                    ),
                    style = AppTypography.title,
                )
            }
        }
    }
}
