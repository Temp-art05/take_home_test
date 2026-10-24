package com.takehome.drama.feature.main.presentation.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.takehome.drama.core.designsystem.component.AppTopBar
import com.takehome.drama.core.designsystem.component.MediaListRow
import com.takehome.drama.core.designsystem.component.RowChevron
import com.takehome.drama.core.designsystem.component.RowTrailing
import com.takehome.drama.core.designsystem.component.SettingsRow
import com.takehome.drama.core.designsystem.component.SettingsRows
import com.takehome.drama.core.designsystem.component.SkeletonBlock
import com.takehome.drama.core.designsystem.component.SkeletonHost
import com.takehome.drama.core.designsystem.icon.AppIcons
import com.takehome.drama.core.designsystem.theme.AppTheme
import com.takehome.drama.core.designsystem.theme.AppTypography
import com.takehome.drama.core.navigation.core.NavigationRequest
import com.takehome.drama.core.navigation.destination.WatchRoutes
import com.takehome.drama.core.ui.foundation.ext.CollectAsEffect
import com.takehome.drama.core.ui.foundation.format.formatCount
import com.takehome.drama.domain.usecase.history.HistoryRecency
import com.takehome.drama.feature.main.R
import com.takehome.drama.feature.main.navigation.HistoryScreenProvider
import com.takehome.drama.feature.main.navigation.SavedScreenProvider
import com.takehome.drama.feature.main.presentation.common.UiErrorState
import com.takehome.drama.feature.main.presentation.library.model.HistoryRow
import com.takehome.drama.feature.main.presentation.library.model.meta
import org.koin.compose.viewmodel.koinViewModel

/** SCR-0401 — tab Library: số đếm, History/Saved, Recent. */
@Composable
internal fun LibraryScreen(
    watchRoutes: WatchRoutes,
    onNavigate: (NavigationRequest) -> Unit,
    viewModel: LibraryViewModel = koinViewModel(),
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()

    viewModel.singleEvent.CollectAsEffect { event ->
        when (event) {
            LibrarySingleEvent.OpenHistory -> onNavigate(NavigationRequest.NavigateTo(HistoryScreenProvider.History))
            LibrarySingleEvent.OpenSaved -> onNavigate(NavigationRequest.NavigateTo(SavedScreenProvider.Saved))
            is LibrarySingleEvent.OpenPlayer ->
                onNavigate(NavigationRequest.NavigateTo(watchRoutes.player(event.showId, event.episodeIndex)))
        }
    }

    LibraryContent(viewState = viewState, onIntent = viewModel::processIntent)
}

@Composable
private fun LibraryContent(
    viewState: LibraryViewState,
    onIntent: (LibraryIntent) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        AppTopBar(title = stringResource(R.string.tab_library), onBack = null)
        val locale = LocalLocale.current.platformLocale
        when {
            viewState.error != null -> UiErrorState(error = viewState.error, onRetry = { onIntent(LibraryIntent.Retry) })
            viewState.isLoading -> LibrarySkeleton()
            else -> LazyColumn(Modifier.fillMaxSize()) {
                item(key = "stats") {
                    StatCard(watched = viewState.watchedCount, saved = viewState.savedCount)
                }
                item(key = "rows") {
                    SettingsRows(topPadding = AppTheme.dimensions.s3) {
                        SettingsRow(
                            label = stringResource(R.string.library_history_row),
                            icon = AppIcons.History,
                            trailing = RowTrailing.Chevron(formatCount(viewState.watchedCount.toLong(), locale)),
                            onClick = { onIntent(LibraryIntent.OpenHistory) },
                        )
                        SettingsRow(
                            label = stringResource(R.string.library_saved),
                            icon = AppIcons.Bookmark,
                            trailing = RowTrailing.Chevron(formatCount(viewState.savedCount.toLong(), locale)),
                            onClick = { onIntent(LibraryIntent.OpenSaved) },
                            divider = false,
                        )
                    }
                }
                if (viewState.recent.isNotEmpty()) {
                    item(key = "recent-head") {
                        RecentHeader(onSeeAll = { onIntent(LibraryIntent.OpenHistory) })
                    }
                    items(viewState.recent, key = { "recent-${it.showId}" }) { row ->
                        MediaListRow(
                            imageUrl = row.posterUrl,
                            title = row.title,
                            meta = row.meta(),
                            progress = row.progress,
                            onClick = { onIntent(LibraryIntent.OpenRecent(row.showId, row.episodeIndex)) },
                            trailing = { RowChevron() },
                        )
                    }
                }
                item(key = "bottom") { Box(Modifier.height(AppTheme.dimensions.s4)) }
            }
        }
    }
}

/** `.mt`: hai số Watched / Saved trên nền surface, vạch ngăn giữa. */
@Composable
private fun StatCard(watched: Int, saved: Int) {
    Row(
        Modifier
            .padding(start = AppTheme.dimensions.s4, end = AppTheme.dimensions.s4, top = AppTheme.dimensions.s1)
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, AppTheme.shapes.md)
            .padding(vertical = AppTheme.dimensions.s4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Stat(value = watched, label = stringResource(R.string.library_watched), modifier = Modifier.weight(1f))
        Box(
            Modifier
                .width(1.dp)
                .height(38.dp)
                .background(MaterialTheme.colorScheme.outline),
        )
        Stat(value = saved, label = stringResource(R.string.library_saved), modifier = Modifier.weight(1f))
    }
}

@Composable
private fun Stat(value: Int, label: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = formatCount(value.toLong(), LocalLocale.current.platformLocale), style = AppTypography.statValue, color = MaterialTheme.colorScheme.onSurface)
        Text(
            text = label,
            style = AppTypography.statLabel,
            color = AppTheme.colors.text3,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}

/** `.row-head`: "Recent" + "See all ›" (cả cụm bấm được, cao 48dp). */
@Composable
private fun RecentHeader(onSeeAll: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = AppTheme.dimensions.s4, end = AppTheme.dimensions.s2, top = AppTheme.dimensions.s2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.library_recent),
            style = AppTypography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        Row(
            Modifier
                .defaultMinSize(minHeight = AppTheme.dimensions.touchTarget)
                .clip(AppTheme.shapes.full)
                .clickable(role = Role.Button, onClick = onSeeAll)
                .padding(horizontal = AppTheme.dimensions.s2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = stringResource(R.string.library_see_all), style = AppTypography.label, color = AppTheme.colors.text3)
            Icon(AppIcons.ChevronEnd, contentDescription = null, tint = AppTheme.colors.text3, modifier = Modifier.size(14.dp))
        }
    }
}

/** 0601 của tab: đúng chỗ thẻ số, 2 hàng, row-head và 2 dòng Recent. */
@Composable
private fun LibrarySkeleton() {
    val dimensions = AppTheme.dimensions
    SkeletonHost {
        Column(Modifier.padding(horizontal = dimensions.s4)) {
            SkeletonBlock(Modifier.padding(top = dimensions.s1).fillMaxWidth().height(76.dp), AppTheme.shapes.md)
            SkeletonBlock(Modifier.padding(top = dimensions.s3).fillMaxWidth().height(110.dp), AppTheme.shapes.md)
            SkeletonBlock(Modifier.padding(top = 26.dp).width(90.dp).height(19.dp))
            repeat(2) { SkeletonListRow(Modifier.padding(top = dimensions.s4)) }
        }
    }
}

/** Khung một dòng `.li` — dùng chung History/Saved. */
@Composable
internal fun SkeletonListRow(modifier: Modifier = Modifier) {
    val dimensions = AppTheme.dimensions
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(dimensions.s3)) {
        SkeletonBlock(Modifier.size(dimensions.listThumbWidth, dimensions.listThumbHeight))
        Column(Modifier.weight(1f)) {
            SkeletonBlock(Modifier.fillMaxWidth(0.8f).height(12.dp))
            SkeletonBlock(Modifier.padding(top = 8.dp).fillMaxWidth(0.55f).height(10.dp))
        }
    }
}

@Preview(heightDp = 700)
@Composable
private fun LibraryContentPreview() {
    val row = HistoryRow("a", "The Masked CEO", "", 11, 12, 74, 0.62f, HistoryRecency.MinutesLeft(4), finished = false)
    AppTheme {
        LibraryContent(
            viewState = LibraryViewState(isLoading = false, watchedCount = 37, savedCount = 12, recent = listOf(row, row.copy(showId = "b"))),
            onIntent = {},
        )
    }
}

