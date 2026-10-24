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
import com.takehome.drama.core.navigation.core.NavigationRequest
import com.takehome.drama.core.navigation.destination.WatchRoutes
import com.takehome.drama.core.ui.foundation.ext.CollectAsEffect
import com.takehome.drama.domain.usecase.history.HistoryRecency
import com.takehome.drama.feature.main.R
import com.takehome.drama.feature.main.navigation.MainScreenProvider
import com.takehome.drama.feature.main.presentation.common.HeaderTextAction
import com.takehome.drama.feature.main.presentation.common.RowActionsSheet
import com.takehome.drama.feature.main.presentation.common.SheetAction
import com.takehome.drama.feature.main.presentation.common.UiErrorState
import com.takehome.drama.feature.main.presentation.library.SkeletonListRow
import com.takehome.drama.feature.main.presentation.library.model.HistoryRow
import com.takehome.drama.feature.main.presentation.library.model.meta
import com.takehome.drama.feature.main.presentation.main.model.MainTab

/** SCR-0402 — Watching / Finished. */
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    watchRoutes: WatchRoutes,
    onNavigate: (NavigationRequest) -> Unit,
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    val toastState = rememberToastState()
    val resources = LocalResources.current

    viewModel.singleEvent.CollectAsEffect { event ->
        when (event) {
            HistorySingleEvent.NavigateBack -> onNavigate(NavigationRequest.PopBackStack)
            is HistorySingleEvent.OpenPlayer ->
                onNavigate(NavigationRequest.NavigateTo(watchRoutes.player(event.showId, event.episodeIndex)))
            is HistorySingleEvent.OpenDetail -> onNavigate(NavigationRequest.NavigateTo(watchRoutes.showDetail(event.showId)))
            is HistorySingleEvent.ShowToast -> toastState.show(resources.getString(event.messageRes), event.success)
            // Launcher debug mở thẳng màn này (không có Main bên dưới) → dựng Main ở tab Browse.
            HistorySingleEvent.GoBrowse ->
                onNavigate(NavigationRequest.NavigateUp(fallBackDestination = MainScreenProvider.Main(MainTab.Browse.name)))
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        HistoryContent(viewState = viewState, onIntent = viewModel::processIntent)
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
    onIntent: (HistoryIntent) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        AppTopBar(
            title = stringResource(R.string.library_history),
            onBack = { onIntent(HistoryIntent.Back) },
            backContentDescription = stringResource(R.string.common_back),
            actions = {
                if (viewState.rows.isNotEmpty()) {
                    HeaderTextAction(
                        text = stringResource(if (viewState.editing) R.string.common_done else R.string.common_edit),
                        onClick = { onIntent(HistoryIntent.ToggleEdit) },
                    )
                }
            },
        )
        if (viewState.error != null) {
            UiErrorState(error = viewState.error, onRetry = { onIntent(HistoryIntent.Retry) })
            return@Column
        }
        SegmentedTabs(
            labels = listOf(stringResource(R.string.history_tab_watching), stringResource(R.string.history_tab_finished)),
            selectedIndex = viewState.tab.ordinal,
            onSelect = { onIntent(HistoryIntent.SelectTab(HistoryTab.entries[it])) },
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
                onAction = { onIntent(HistoryIntent.FindNext) },
            )
            viewState.rows.isEmpty() -> BlankState(
                glyph = AppIcons.Check,
                title = stringResource(R.string.history_empty_finished_title),
                body = stringResource(R.string.history_empty_finished_body),
            )
            else -> LazyColumn(Modifier.fillMaxSize().padding(top = AppTheme.dimensions.s2)) {
                items(viewState.rows, key = { it.showId }) { row ->
                    HistoryListRow(row = row, editing = viewState.editing, onIntent = onIntent)
                }
            }
        }
    }
}

@Composable
private fun HistoryListRow(row: HistoryRow, editing: Boolean, onIntent: (HistoryIntent) -> Unit) {
    MediaListRow(
        imageUrl = row.posterUrl,
        title = row.title,
        meta = row.meta(),
        progress = row.progress,
        showInlineProgress = true,
        onClick = { onIntent(HistoryIntent.OpenRow(row)) },
        trailing = {
            if (editing) {
                RowEndButton(
                    icon = AppIcons.Close,
                    contentDescription = stringResource(R.string.common_remove_cd, row.title),
                    onClick = { onIntent(HistoryIntent.Remove(row.showId)) },
                )
            } else {
                RowEndButton(
                    icon = AppIcons.More,
                    contentDescription = stringResource(R.string.common_more_cd, row.title),
                    onClick = { onIntent(HistoryIntent.OpenMenu(row)) },
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
            onIntent = {},
        )
    }
}
