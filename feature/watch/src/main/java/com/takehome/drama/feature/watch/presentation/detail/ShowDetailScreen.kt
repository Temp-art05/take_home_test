package com.takehome.drama.feature.watch.presentation.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.takehome.drama.core.designsystem.component.AppAsyncImage
import com.takehome.drama.core.designsystem.component.AppToastHost
import com.takehome.drama.core.designsystem.component.ChoiceChip
import com.takehome.drama.core.designsystem.component.ErrorState
import com.takehome.drama.core.designsystem.component.GhostIconButton
import com.takehome.drama.core.designsystem.component.GradientButton
import com.takehome.drama.core.designsystem.component.LoadingState
import com.takehome.drama.core.designsystem.component.OutlinePillButton
import com.takehome.drama.core.designsystem.component.rememberToastState
import com.takehome.drama.core.designsystem.ext.applyIf
import com.takehome.drama.core.designsystem.theme.AppTheme
import com.takehome.drama.core.designsystem.theme.AppTypography
import com.takehome.drama.core.navigation.core.NavigationRequest
import com.takehome.drama.core.ui.foundation.ext.CollectAsEffect
import com.takehome.drama.core.ui.foundation.format.formatCompactCount
import com.takehome.drama.domain.usecase.watch.EpisodeCell
import com.takehome.drama.domain.usecase.watch.EpisodeCellState
import com.takehome.drama.domain.usecase.watch.EpisodeRange
import com.takehome.drama.feature.watch.R
import com.takehome.drama.feature.watch.navigation.PlayerScreenProvider
import com.takehome.drama.feature.watch.presentation.common.EpisodeGridCell

private const val GRID_COLUMNS = 5


private val HeroHeight = 296.dp
private val CellGap = 9.dp

@Composable
fun ShowDetailScreen(
    viewModel: ShowDetailViewModel,
    onNavigate: (NavigationRequest) -> Unit,
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    val gridState = rememberLazyGridState()
    val toastState = rememberToastState()
    val resources = LocalResources.current

    viewModel.singleEvent.CollectAsEffect { event ->
        when (event) {
            ShowDetailSingleEvent.NavigateBack -> onNavigate(NavigationRequest.PopBackStack)
            is ShowDetailSingleEvent.OpenPlayer -> onNavigate(
                NavigationRequest.NavigateTo(PlayerScreenProvider.Player(event.showId, event.episodeIndex))
            )
            is ShowDetailSingleEvent.ShowToast -> toastState.show(
                text = event.formatArg?.let { resources.getString(event.messageRes, it) } ?: resources.getString(event.messageRes),
                success = event.success,
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        ShowDetailContent(
            viewState = viewState,
            gridState = gridState,
            onIntent = viewModel::processIntent,
        )
        AppToastHost(
            state = toastState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = AppTheme.dimensions.s6),
        )
    }
}

@Composable
private fun ShowDetailContent(
    viewState: ShowDetailViewState,
    gridState: LazyGridState,
    onIntent: (ShowDetailIntent) -> Unit,
) {
    when {
        viewState.isLoading -> LoadingState()
        viewState.errorRes != null -> Column(Modifier.fillMaxSize()) {
            BackButton(onClick = { onIntent(ShowDetailIntent.BackClick) }, modifier = Modifier.statusBarsPadding())
            val notFound = viewState.errorRes == R.string.watch_show_not_found
            ErrorState(
                message = stringResource(viewState.errorRes),
                retryLabel = stringResource(if (notFound) R.string.watch_back else R.string.watch_retry),
                onRetry = { onIntent(if (notFound) ShowDetailIntent.BackClick else ShowDetailIntent.RetryClick) },
            )
        }
        else -> DetailGrid(viewState, gridState, onIntent)
    }
}

@Composable
private fun DetailGrid(
    viewState: ShowDetailViewState,
    gridState: LazyGridState,
    onIntent: (ShowDetailIntent) -> Unit,
) {
    val gutter = AppTheme.dimensions.screenGutter
    // Một LazyVerticalGrid cho cả trang: header là item full-span, lưới tập cuộn chung.
    LazyVerticalGrid(
        columns = GridCells.Fixed(GRID_COLUMNS),
        state = gridState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = gutter,
            end = gutter,
            bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + AppTheme.dimensions.s6,
        ),
        horizontalArrangement = Arrangement.spacedBy(CellGap),
        verticalArrangement = Arrangement.spacedBy(CellGap),
    ) {
        fullSpan { Hero(viewState, gutter, onBack = { onIntent(ShowDetailIntent.BackClick) }) }
        fullSpan { Stats(viewState) }
        fullSpan {
            Synopsis(
                text = viewState.synopsis,
                expanded = viewState.synopsisExpanded,
                onToggle = { onIntent(ShowDetailIntent.ToggleSynopsis) },
            )
        }
        fullSpan { CtaRow(viewState, onIntent) }
        fullSpan {
            EpisodesHeader(
                ranges = viewState.ranges,
                selected = viewState.selectedRange,
                episodeCount = viewState.cells.size,
                watchedCount = viewState.watchedCount,
                onSelect = { onIntent(ShowDetailIntent.RangeSelect(it)) },
            )
        }
        if (viewState.episodesErrorRes != null) {
            fullSpan {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.watch_episodes_error),
                        style = AppTypography.body,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(AppTheme.dimensions.s3))
                    OutlinePillButton(
                        text = stringResource(R.string.watch_retry),
                        onClick = { onIntent(ShowDetailIntent.RetryEpisodesClick) },
                    )
                }
            }
        } else if (viewState.isEpisodesLoading) {
            fullSpan { LoadingState(Modifier.height(120.dp)) }
        } else {
            items(viewState.visibleCells, key = { it.index }) { cell ->
                EpisodeGridCell(cell = cell, onClick = { onIntent(ShowDetailIntent.EpisodeClick(it)) })
            }
        }
    }
}

private fun LazyGridScope.fullSpan(content: @Composable () -> Unit) {
    item(span = { GridItemSpan(maxLineSpan) }) { content() }
}

@Composable
private fun Hero(viewState: ShowDetailViewState, gutter: Dp, onBack: () -> Unit) {
    // Grid có lề 16dp; hero tràn hết chiều ngang nên đo rộng thêm hai lề rồi đặt lùi về trái.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .layout { measurable, constraints ->
                val extra = (gutter * 2).roundToPx()
                val width = constraints.maxWidth + extra
                val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
                layout(constraints.maxWidth, placeable.height) { placeable.place(-extra / 2, 0) }
            }
            .height(HeroHeight),
    ) {
        AppAsyncImage(
            model = viewState.posterUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            placeholder = { Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant)) },
        )
        Box(Modifier.fillMaxWidth().height(80.dp).background(AppTheme.colors.scrimTop))
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(HeroHeight * 0.56f)
                .background(AppTheme.colors.scrimBottom),
        )
        BackButton(
            onClick = onBack,
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = AppTheme.dimensions.s1, top = AppTheme.dimensions.s1),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = gutter)
                .padding(bottom = AppTheme.dimensions.s4),
        ) {
            Text(
                text = viewState.title,
                style = AppTypography.display.copy(fontSize = 27.sp, lineHeight = 30.sp),
                color = AppTheme.colors.media.text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (viewState.tags.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    viewState.tags.forEach { TagChip(it) }
                }
            }
        }
    }
}

@Composable
private fun TagChip(text: String) {
    Text(
        text = text,
        style = AppTypography.captionSemibold.copy(fontSize = 11.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .border(1.dp, AppTheme.colors.media.glassBorder, AppTheme.shapes.full)
            .padding(horizontal = 9.dp, vertical = 4.dp),
    )
}

@Composable
private fun BackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.watch_back),
            tint = AppTheme.colors.media.text,
        )
    }
}

@Composable
private fun Stats(viewState: ShowDetailViewState) {
    val locale = Locale.current.platformLocale
    Row(
        modifier = Modifier.padding(top = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.dimensions.s6),
    ) {
        Stat(value = viewState.episodeTotal.toString(), label = stringResource(R.string.watch_stat_episodes))
        Stat(value = formatCompactCount(viewState.viewCount, locale), label = stringResource(R.string.watch_stat_views))
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Column {
        Text(text = value, style = AppTypography.button, color = MaterialTheme.colorScheme.onBackground)
        Text(
            text = label,
            style = AppTypography.caption.copy(fontSize = 11.sp),
            color = AppTheme.colors.text3,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun Synopsis(text: String, expanded: Boolean, onToggle: () -> Unit) {
    if (text.isBlank()) return
    val more = stringResource(if (expanded) R.string.watch_less else R.string.watch_more)
    val emphasis = MaterialTheme.colorScheme.onBackground
    // "More" chỉ khi 3 dòng thật sự cắt mất chữ.
    var truncated by remember(text) { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .padding(top = 14.dp)
            .applyIf(truncated || expanded) { clickable(role = Role.Button, onClickLabel = more, onClick = onToggle) },
    ) {
        Text(
            text = text,
            style = AppTypography.body.copy(fontSize = 13.5.sp, lineHeight = 21.6.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = if (expanded) Int.MAX_VALUE else 3,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { if (!expanded) truncated = it.hasVisualOverflow },
        )
        if (truncated || expanded) {
            Text(
                text = buildAnnotatedString { withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(more) } },
                style = AppTypography.body.copy(fontSize = 13.5.sp),
                color = emphasis,
            )
        }
    }
}

@Composable
private fun CtaRow(viewState: ShowDetailViewState, onIntent: (ShowDetailIntent) -> Unit) {
    Row(
        modifier = Modifier.padding(top = AppTheme.dimensions.s4, bottom = AppTheme.dimensions.s1),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val cta = viewState.continueCta
        GradientButton(
            text = when {
                cta == null -> stringResource(R.string.watch_play_episode, 1)
                // Tập kế bị khoá: nói thẳng, không hứa "Continue" rồi chỉ hiện toast.
                cta.isLocked -> stringResource(R.string.watch_locked_title, cta.episodeNumber)
                cta.isResume -> stringResource(R.string.watch_continue_episode, cta.episodeNumber)
                else -> stringResource(R.string.watch_play_episode, cta.episodeNumber)
            },
            onClick = { onIntent(ShowDetailIntent.ContinueClick) },
            enabled = cta != null,
            modifier = Modifier.weight(1f),
            leadingIcon = {
                Icon(
                    imageVector = if (cta?.isLocked == true) Icons.Filled.Lock else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp),
                )
            },
        )
        GhostIconButton(onClick = { onIntent(ShowDetailIntent.SaveClick) }) {
            Icon(
                imageVector = if (viewState.isSaved) Icons.Filled.Check else Icons.Filled.Add,
                contentDescription = stringResource(if (viewState.isSaved) R.string.watch_saved_cd else R.string.watch_save_cd),
                tint = AppTheme.colors.media.text,
            )
        }
    }
}

/**
 * "Episodes" + "44 eps · 7 watched", dưới là tab khoảng 20 tập (cùng kiểu chip với sheet 0303). Tab lọc
 * lưới — chọn tab là thấy ngay đúng các tập đó, không phải cuộn tìm.
 */
@Composable
private fun EpisodesHeader(
    ranges: List<EpisodeRange>,
    selected: EpisodeRange?,
    episodeCount: Int,
    watchedCount: Int,
    onSelect: (EpisodeRange) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = AppTheme.dimensions.s5, bottom = AppTheme.dimensions.s1),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = stringResource(R.string.watch_episodes),
                style = AppTypography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            if (episodeCount > 0) {
                Text(
                    text = stringResource(
                        R.string.watch_episodes_summary,
                        pluralStringResource(R.plurals.watch_episode_count, episodeCount, episodeCount),
                        watchedCount,
                    ),
                    style = AppTypography.label,
                    color = AppTheme.colors.text3,
                )
            }
        }
        if (ranges.size > 1) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(top = AppTheme.dimensions.s3),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.dimensions.s2),
            ) {
                ranges.forEach { range ->
                    ChoiceChip(
                        text = stringResource(R.string.watch_range, range.firstLabel, range.lastLabel),
                        selected = range == selected,
                        onClick = { onSelect(range) },
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun ShowDetailContentPreview() {
    AppTheme {
        Box(Modifier.background(MaterialTheme.colorScheme.background)) {
            ShowDetailContent(
                viewState = ShowDetailViewState(
                    isLoading = false,
                    isEpisodesLoading = false,
                    title = "The Ex-Wife Returns",
                    tags = listOf("Romance", "Revenge"),
                    episodeTotal = 32,
                    viewCount = 12_400_000,
                    synopsis = "Thrown out of her husband's house on a rainy night, Lam Vy returns three years later.",
                    cells = List(12) {
                        EpisodeCell(it, it + 1, if (it < 4) EpisodeCellState.Watched else if (it == 4) EpisodeCellState.Playing else EpisodeCellState.Locked)
                    },
                    ranges = listOf(EpisodeRange(0, 20, 1, 20), EpisodeRange(20, 32, 21, 32)),
                    continueCta = ContinueCta(4, 5, isResume = true, isLocked = false),
                ),
                gridState = rememberLazyGridState(),
                onIntent = {},
            )
        }
    }
}
