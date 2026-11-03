package com.takehome.drama.feature.main.presentation.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.takehome.drama.core.designsystem.component.AppToastHost
import com.takehome.drama.core.designsystem.component.AppTopBar
import com.takehome.drama.core.designsystem.component.BlankState
import com.takehome.drama.core.designsystem.component.MediaListRow
import com.takehome.drama.core.designsystem.component.RowEndButton
import com.takehome.drama.core.designsystem.component.SegmentedTabs
import com.takehome.drama.core.designsystem.component.SkeletonHost
import com.takehome.drama.core.designsystem.component.rememberToastState
import com.takehome.drama.core.designsystem.icon.AppIcons
import com.takehome.drama.core.designsystem.theme.AppTheme
import com.takehome.drama.core.ui.foundation.ext.CollectAsEffect
import com.takehome.drama.domain.usecase.history.HistoryRecency
import com.takehome.drama.feature.main.R
import com.takehome.drama.feature.main.presentation.common.HeaderTextAction
import com.takehome.drama.feature.main.presentation.common.RowActionsSheet
import com.takehome.drama.feature.main.presentation.common.SheetAction
import com.takehome.drama.feature.main.presentation.common.UiErrorState
import com.takehome.drama.feature.main.presentation.main.library.SkeletonListRow
import com.takehome.drama.feature.main.presentation.main.library.model.HistoryRow
import com.takehome.drama.feature.main.presentation.main.library.model.meta

/** SCR-0402 — Watching / Finished. */
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onBack: () -> Unit,
    onOpenPlayer: (showId: String, episodeIndex: Int) -> Unit,
    onOpenShowDetail: (showId: String) -> Unit,
    onOpenBrowseTab: () -> Unit,
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    val toastState = rememberToastState()
    val resources = LocalResources.current

    viewModel.singleEvent.CollectAsEffect { event ->
        when (event) {
            HistorySingleEvent.NavigateBack -> onBack()
            is HistorySingleEvent.OpenPlayer -> onOpenPlayer(event.showId, event.episodeIndex)
            is HistorySingleEvent.OpenDetail -> onOpenShowDetail(event.showId)
            is HistorySingleEvent.ShowToast -> toastState.show(resources.getString(event.messageRes), event.success)
            HistorySingleEvent.GoBrowse -> onOpenBrowseTab()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        HistoryContent(
            viewState = viewState,
            onBackClick = { viewModel.processIntent(HistoryIntent.Back) },
            onEditToggle = { viewModel.processIntent(HistoryIntent.ToggleEdit) },
            onRetry = { viewModel.processIntent(HistoryIntent.Retry) },
            onTabSelect = { tab -> viewModel.processIntent(HistoryIntent.SelectTab(tab)) },
            onFindNext = { viewModel.processIntent(HistoryIntent.FindNext) },
            onRowClick = { row -> viewModel.processIntent(HistoryIntent.OpenRow(row)) },
            onRowRemove = { showId -> viewModel.processIntent(HistoryIntent.Remove(showId)) },
            onRowMenuClick = { row -> viewModel.processIntent(HistoryIntent.OpenMenu(row)) },
        )
        AppToastHost(
            state = toastState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = AppTheme.dimensions.s4),
        )
        val menuRow = viewState.menuFor
        RowActionsSheet(
            title = menuRow?.title,
            actions = if (menuRow == null) {
                emptyList()
            } else {
                listOf(
                    SheetAction(AppIcons.ChevronEnd, stringResource(R.string.history_sheet_details)) {
                        viewModel.processIntent(HistoryIntent.OpenDetails(menuRow.showId))
                    },
                    SheetAction(AppIcons.Close, stringResource(R.string.history_sheet_remove)) {
                        viewModel.processIntent(HistoryIntent.Remove(menuRow.showId))
                    },
                )
            },
            onDismiss = { viewModel.processIntent(HistoryIntent.DismissMenu) },
        )
    }
}

@Composable
private fun HistoryContent(
    viewState: HistoryViewState,
    onBackClick: () -> Unit,
    onEditToggle: () -> Unit,
    onRetry: () -> Unit,
    onTabSelect: (HistoryTab) -> Unit,
    onFindNext: () -> Unit,
    onRowClick: (HistoryRow) -> Unit,
    onRowRemove: (showId: String) -> Unit,
    onRowMenuClick: (HistoryRow) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        AppTopBar(
            title = stringResource(R.string.library_history),
            onBack = onBackClick,
            backContentDescription = stringResource(R.string.common_back),
            actions = {
                if (viewState.rows.isNotEmpty()) {
                    HeaderTextAction(
                        text = stringResource(if (viewState.editing) R.string.common_done else R.string.common_edit),
                        onClick = onEditToggle,
                    )
                }
            },
        )
        if (viewState.error != null) {
            UiErrorState(error = viewState.error, onRetry = onRetry)
            return@Column
        }
        SegmentedTabs(
            labels = listOf(stringResource(R.string.history_tab_watching), stringResource(R.string.history_tab_finished)),
            selectedIndex = viewState.tab.ordinal,
            onSelect = { onTabSelect(HistoryTab.entries[it]) },
            modifier = Modifier.padding(top = AppTheme.dimensions.s2),
        )
        when {
            viewState.isLoading -> SkeletonHost {
                Column(Modifier.padding(horizontal = AppTheme.dimensions.s4, vertical = AppTheme.dimensions.s5)) {
                    repeat(5) { SkeletonListRow(Modifier.padding(bottom = AppTheme.dimensions.s6)) }
                }
            }
            viewState.rows.isEmpty() && viewState.tab == HistoryTab.Watching -> BlankState(
                glyph = AppIcons.History,
                title = stringResource(R.string.history_empty_watching_title),
                body = stringResource(R.string.history_empty_watching_body),
                actionLabel = stringResource(R.string.common_find_next),
                onAction = onFindNext,
            )
            viewState.rows.isEmpty() -> BlankState(
                glyph = AppIcons.Check,
                title = stringResource(R.string.history_empty_finished_title),
                body = stringResource(R.string.history_empty_finished_body),
            )
            else -> LazyColumn(Modifier.fillMaxSize().padding(top = AppTheme.dimensions.s2)) {
                items(viewState.rows, key = { it.showId }) { row ->
                    HistoryListRow(
                        row = row,
                        editing = viewState.editing,
                        onClick = { onRowClick(row) },
                        onRemove = { onRowRemove(row.showId) },
                        onMenuClick = { onRowMenuClick(row) },
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryListRow(
    row: HistoryRow,
    editing: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    onMenuClick: () -> Unit,
) {
    MediaListRow(
        imageUrl = row.posterUrl,
        title = row.title,
        meta = row.meta(),
        progress = row.progress,
        showInlineProgress = true,
        onClick = onClick,
        trailing = {
            if (editing) {
                RowEndButton(
                    icon = AppIcons.Close,
                    contentDescription = stringResource(R.string.common_remove_cd, row.title),
                    onClick = onRemove,
                )
            } else {
                RowEndButton(
                    icon = AppIcons.More,
                    contentDescription = stringResource(R.string.common_more_cd, row.title),
                    onClick = onMenuClick,
                )
            }
        },
    )
}

@Preview(heightDp = 700)
@Composable
private fun HistoryContentPreview() {
    val row = HistoryRow("a", "The Masked CEO", "", 11, 12, 74, 0.62f, HistoryRecency.MinutesLeft(4), finished = false)
    AppTheme {
        HistoryContent(
            viewState = HistoryViewState(
                isLoading = false,
                watching = listOf(row, row.copy(showId = "b", recency = HistoryRecency.Yesterday, progress = 0.19f)),
            ),
            onBackClick = {},
            onEditToggle = {},
            onRetry = {},
            onTabSelect = {},
            onFindNext = {},
            onRowClick = {},
            onRowRemove = {},
            onRowMenuClick = {},
        )
    }
}
