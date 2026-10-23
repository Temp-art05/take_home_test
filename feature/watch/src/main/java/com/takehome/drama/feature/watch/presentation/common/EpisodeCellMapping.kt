package com.takehome.drama.feature.watch.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.takehome.drama.core.designsystem.component.EpisodeCell
import com.takehome.drama.core.designsystem.component.EpisodeCellStyle
import com.takehome.drama.domain.usecase.watch.EpisodeCellState
import com.takehome.drama.feature.watch.R
import com.takehome.drama.domain.usecase.watch.EpisodeCell as EpisodeCellModel

/** Ô tập dùng chung Detail và sheet 0303 — map trạng thái domain sang style của design system. */
@Composable
internal fun EpisodeGridCell(
    cell: EpisodeCellModel,
    onClick: (index: Int) -> Unit,
    modifier: Modifier = Modifier,
    onSheet: Boolean = false,
) {
    EpisodeCell(
        label = cell.label.toString(),
        style = when (cell.state) {
            EpisodeCellState.Locked -> EpisodeCellStyle.Locked
            EpisodeCellState.Playing -> EpisodeCellStyle.Playing
            EpisodeCellState.Watched -> EpisodeCellStyle.Watched
            EpisodeCellState.Unwatched -> EpisodeCellStyle.Unwatched
        },
        onClick = { onClick(cell.index) },
        modifier = modifier,
        onSheet = onSheet,
        lockedDescription = stringResource(R.string.watch_locked_cd),
    )
}
