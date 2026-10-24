package com.takehome.drama.feature.main.presentation.saved

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.takehome.drama.core.designsystem.component.AppToastHost
import com.takehome.drama.core.designsystem.component.AppTopBar
import com.takehome.drama.core.designsystem.component.BlankState
import com.takehome.drama.core.designsystem.component.ChoiceChip
import com.takehome.drama.core.designsystem.component.MediaListRow
import com.takehome.drama.core.designsystem.component.RowEndButton
import com.takehome.drama.core.designsystem.component.SkeletonHost
import com.takehome.drama.core.designsystem.component.rememberToastState
import com.takehome.drama.core.designsystem.icon.AppIcons
import com.takehome.drama.core.designsystem.theme.AppTheme
import com.takehome.drama.core.designsystem.theme.AppTypography
import com.takehome.drama.core.navigation.core.NavigationRequest
import com.takehome.drama.core.navigation.destination.WatchRoutes
import com.takehome.drama.core.ui.foundation.ext.CollectAsEffect
import com.takehome.drama.domain.model.ShowStatus
import com.takehome.drama.domain.usecase.library.SavedFilter
import com.takehome.drama.feature.main.R
import com.takehome.drama.feature.main.navigation.MainScreenProvider
import com.takehome.drama.feature.main.presentation.common.HeaderTextAction
import com.takehome.drama.feature.main.presentation.common.RowActionsSheet
import com.takehome.drama.feature.main.presentation.common.SheetAction
import com.takehome.drama.feature.main.presentation.common.UiErrorState
import com.takehome.drama.feature.main.presentation.library.SkeletonListRow
import com.takehome.drama.feature.main.presentation.library.model.SavedRow
import com.takehome.drama.feature.main.presentation.library.model.meta
import com.takehome.drama.feature.main.presentation.main.model.MainTab

/** SCR-0403 — show đã lưu; chưa lưu gì → SCR-0602. */
@Composable
fun SavedScreen(
    viewModel: SavedViewModel,
    watchRoutes: WatchRoutes,
    onNavigate: (NavigationRequest) -> Unit,
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    val toastState = rememberToastState()
    val resources = LocalResources.current

    viewModel.singleEvent.CollectAsEffect { event ->
        when (event) {
            SavedSingleEvent.NavigateBack -> onNavigate(NavigationRequest.PopBackStack)
            is SavedSingleEvent.OpenDetail -> onNavigate(NavigationRequest.NavigateTo(watchRoutes.showDetail(event.showId)))
            is SavedSingleEvent.ShowToast -> toastState.show(resources.getString(event.messageRes), event.success)
            SavedSingleEvent.GoBrowse ->
                onNavigate(NavigationRequest.NavigateUp(fallBackDestination = MainScreenProvider.Main(MainTab.Browse.name)))
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        SavedContent(viewState = viewState, onIntent = viewModel::processIntent)
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
                        viewModel.processIntent(SavedIntent.OpenRow(menuRow.showId))
                    },
                    SheetAction(AppIcons.Close, stringResource(R.string.saved_sheet_remove)) {
                        viewModel.processIntent(SavedIntent.Remove(menuRow.showId))
                    },
                )
            },
            onDismiss = { viewModel.processIntent(SavedIntent.DismissMenu) },
        )
    }
}

@Composable
private fun SavedContent(
    viewState: SavedViewState,
    onIntent: (SavedIntent) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        AppTopBar(
            title = stringResource(R.string.library_saved),
            onBack = { onIntent(SavedIntent.Back) },
            backContentDescription = stringResource(R.string.common_back),
            actions = {
                if (viewState.items.isNotEmpty()) {
                    HeaderTextAction(
                        text = stringResource(if (viewState.editing) R.string.common_done else R.string.common_edit),
                        onClick = { onIntent(SavedIntent.ToggleEdit) },
                    )
                }
            },
        )
        when {
            viewState.error != null -> UiErrorState(error = viewState.error, onRetry = { onIntent(SavedIntent.Retry) })
            viewState.isLoading -> SkeletonHost {
                Column(Modifier.padding(horizontal = AppTheme.dimensions.s4, vertical = AppTheme.dimensions.s5)) {
                    repeat(5) { SkeletonListRow(Modifier.padding(bottom = AppTheme.dimensions.s6)) }
                }
            }
            // SCR-0602: không chip, không Edit — một lời mời và một nút đi tiếp.
            viewState.isShelfEmpty -> BlankState(
                glyph = AppIcons.Heart,
                title = stringResource(R.string.saved_empty_title),
                body = stringResource(R.string.saved_empty_body),
                actionLabel = stringResource(R.string.common_find_next),
                onAction = { onIntent(SavedIntent.FindNext) },
            )
            else -> {
                FilterChips(viewState.filter, viewState.totalCount, onSelect = { onIntent(SavedIntent.SelectFilter(it)) })
                if (viewState.items.isEmpty()) {
                    Text(
                        text = stringResource(
                            if (viewState.filter == SavedFilter.Airing) R.string.saved_filter_empty_airing else R.string.saved_filter_empty_complete,
                        ),
                        style = AppTypography.body,
                        color = AppTheme.colors.text3,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 40.dp, vertical = 48.dp),
                    )
                } else {
                    LazyColumn(Modifier.fillMaxSize().padding(top = 2.dp)) {
                        items(viewState.items, key = { it.showId }) { row ->
                            SavedListRow(row = row, editing = viewState.editing, onIntent = onIntent)
                        }
                    }
                }
            }
        }
    }
}

/** Chip giữ nguyên chỗ khi đổi (cùng kích thước, không sắp lại). */
@Composable
private fun FilterChips(selected: SavedFilter, total: Int, onSelect: (SavedFilter) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = AppTheme.dimensions.s4, vertical = AppTheme.dimensions.s1),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.dimensions.s2),
    ) {
        SavedFilter.entries.forEach { filter ->
            ChoiceChip(
                text = when (filter) {
                    SavedFilter.All -> stringResource(R.string.saved_filter_all, total)
                    SavedFilter.Airing -> stringResource(R.string.saved_filter_airing)
                    SavedFilter.Complete -> stringResource(R.string.saved_filter_complete)
                },
                selected = filter == selected,
                onClick = { onSelect(filter) },
            )
        }
    }
}

@Composable
private fun SavedListRow(row: SavedRow, editing: Boolean, onIntent: (SavedIntent) -> Unit) {
    MediaListRow(
        imageUrl = row.posterUrl,
        title = row.title,
        meta = row.meta(),
        onClick = { onIntent(SavedIntent.OpenRow(row.showId)) },
        trailing = {
            if (editing) {
                RowEndButton(
                    icon = AppIcons.Close,
                    contentDescription = stringResource(R.string.common_remove_cd, row.title),
                    onClick = { onIntent(SavedIntent.Remove(row.showId)) },
                )
            } else {
                RowEndButton(
                    icon = AppIcons.More,
                    contentDescription = stringResource(R.string.common_more_cd, row.title),
                    onClick = { onIntent(SavedIntent.OpenMenu(row)) },
                )
            }
        },
    )
}

@Preview(heightDp = 700)
@Composable
private fun SavedContentPreview() {
    val row = SavedRow("a", "Night of Fate", "", "Thriller", 52, ShowStatus.Completed)
    AppTheme {
        SavedContent(
            viewState = SavedViewState(isLoading = false, items = listOf(row, row.copy(showId = "b")), totalCount = 2),
            onIntent = {},
        )
    }
}

@Preview(heightDp = 700)
@Composable
private fun SavedEmptyPreview() {
    AppTheme { SavedContent(viewState = SavedViewState(isLoading = false), onIntent = {}) }
}
