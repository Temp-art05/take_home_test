package com.takehome.drama.feature.main.presentation.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.takehome.drama.core.designsystem.component.AppToastHost
import com.takehome.drama.core.designsystem.component.ChoiceChip
import com.takehome.drama.core.designsystem.component.SkeletonBlock
import com.takehome.drama.core.designsystem.component.SkeletonHost
import com.takehome.drama.core.designsystem.component.GradientButton
import com.takehome.drama.core.designsystem.component.KeyArt
import com.takehome.drama.core.designsystem.component.rememberToastState
import com.takehome.drama.core.designsystem.icon.AppIcons
import com.takehome.drama.core.designsystem.theme.AppTheme
import com.takehome.drama.core.designsystem.theme.AppTypography
import com.takehome.drama.core.media.VideoSlot
import com.takehome.drama.core.media.VideoSurface
import com.takehome.drama.core.navigation.core.NavigationRequest
import com.takehome.drama.core.navigation.destination.WatchRoutes
import com.takehome.drama.core.ui.foundation.ext.CollectAsEffect
import com.takehome.drama.core.utils.extension.findActivity
import com.takehome.drama.domain.model.ShowStatus
import com.takehome.drama.feature.main.R
import com.takehome.drama.feature.main.presentation.common.UiErrorState
import com.takehome.drama.feature.main.navigation.SearchScreenProvider
import com.takehome.drama.feature.main.presentation.common.ShowCard
import com.takehome.drama.feature.main.presentation.common.label
import org.koin.androidx.compose.koinViewModel

/** Poster không được chọn mờ 72% (design). */
private const val UNSELECTED_ALPHA = 0.55f

@Composable
internal fun BrowseScreen(
    watchRoutes: WatchRoutes,
    onNavigate: (NavigationRequest) -> Unit,
    isSelected: Boolean,
    viewModel: BrowseViewModel = koinViewModel(),
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    val slots by viewModel.player.slots.collectAsStateWithLifecycle()
    val toastState = rememberToastState()
    val context = LocalContext.current
    val resources = LocalResources.current
    viewModel.singleEvent.CollectAsEffect { event ->
        when (event) {
            is BrowseSingleEvent.OpenShow -> onNavigate(NavigationRequest.NavigateTo(watchRoutes.showDetail(event.showId)))
            is BrowseSingleEvent.OpenPlayer ->
                onNavigate(NavigationRequest.NavigateTo(watchRoutes.player(event.showId, event.episodeIndex)))
            BrowseSingleEvent.OpenSearch -> onNavigate(NavigationRequest.NavigateTo(SearchScreenProvider.Search()))
            is BrowseSingleEvent.ShowToast -> toastState.show(resources.getString(event.messageRes), success = event.success)
        }
    }
    LaunchedEffect(isSelected) { viewModel.processIntent(BrowseIntent.TabVisible(isSelected)) }
    LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.processIntent(BrowseIntent.ScreenStarted) }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        val changingConfigurations = context.findActivity()?.isChangingConfigurations == true
        viewModel.processIntent(BrowseIntent.ScreenStopped(changingConfigurations))
    }

    Box(Modifier.fillMaxSize()) {
        BrowseContent(viewState = viewState, slot = slots[0], onIntent = viewModel::processIntent)
        AppToastHost(state = toastState, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = AppTheme.dimensions.s4))
    }
}

@Composable
private fun BrowseContent(
    viewState: BrowseViewState,
    slot: VideoSlot?,
    onIntent: (BrowseIntent) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (maxWidth > maxHeight) {
            // Ngang (xoay máy): dock trên-dưới chỉ còn ~150dp, tiêu đề đè header → dock bên trái cao hết màn, lưới bên phải.
            val dockWidth = maxWidth * LANDSCAPE_DOCK_FRACTION
            Row(Modifier.fillMaxSize()) {
                PreviewDock(viewState, slot, Modifier.width(dockWidth).fillMaxHeight(), onIntent)
                Column(Modifier.weight(1f).fillMaxHeight().statusBarsPadding()) {
                    BrowseGrid(viewState, onIntent)
                }
            }
        } else {
            val dockHeight = min(AppTheme.dimensions.previewDock, maxHeight * MAX_DOCK_FRACTION)
            Column(Modifier.fillMaxSize()) {
                PreviewDock(viewState, slot, Modifier.fillMaxWidth().height(dockHeight), onIntent)
                BrowseGrid(viewState, onIntent)
            }
        }
    }
}

@Composable
private fun ColumnScope.BrowseGrid(
    viewState: BrowseViewState,
    onIntent: (BrowseIntent) -> Unit,
) {
    val gutter = AppTheme.dimensions.screenGutter
    if (viewState.error != null) {
        UiErrorState(error = viewState.error, onRetry = { onIntent(BrowseIntent.Retry) }, modifier = Modifier.weight(1f))
        return
    }
    if (viewState.isLoading && viewState.grid.isEmpty()) {
        BrowseGridSkeleton()
        return
    }
    LazyRow(
        contentPadding = PaddingValues(start = gutter, end = gutter, top = AppTheme.dimensions.s3, bottom = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.dimensions.s2),
    ) {
        items(viewState.chips, key = { it.tagId ?: "all" }) { chip ->
            ChoiceChip(
                text = chip.tagId?.let { chip.label } ?: stringResource(R.string.browse_chip_all),
                selected = chip.tagId == viewState.selectedChip,
                onClick = { onIntent(BrowseIntent.SelectChip(chip.tagId)) },
                verticalPadding = 4.dp,
            )
        }
    }
    Text(
        text = stringResource(R.string.browse_hint).uppercase(),
        style = AppTypography.eyebrow.copy(fontSize = 11.sp, letterSpacing = AppTypography.badge.letterSpacing),
        color = AppTheme.colors.text3,
        modifier = Modifier.padding(start = gutter, end = gutter, top = 10.dp),
    )
    LazyVerticalGrid(
        columns = GridCells.Fixed(GRID_COLUMNS),
        contentPadding = PaddingValues(start = gutter, end = gutter, top = AppTheme.dimensions.s2, bottom = AppTheme.dimensions.s4),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
        modifier = Modifier.weight(1f),
    ) {
        items(viewState.grid, key = { it.id }) { show ->
            GridPoster(
                show = show,
                selected = show.id == viewState.preview?.id,
                onClick = { onIntent(BrowseIntent.SelectPoster(show.id)) },
            )
        }
    }
}

/** Tầng trên cố định: video câm (hoặc key art) + scrim + tiêu đề + Play Ep 1 / lưu. */
@Composable
private fun PreviewDock(viewState: BrowseViewState, slot: VideoSlot?, modifier: Modifier, onIntent: (BrowseIntent) -> Unit) {
    val preview = viewState.preview
    val media = AppTheme.colors.media
    val art: @Composable () -> Unit = {
        KeyArt(posterUrl = preview?.posterUrl, seed = preview?.id.orEmpty(), modifier = Modifier.fillMaxSize())
    }
    Box(
        modifier
            // Video crop phủ kín khung lớn hơn dock — không clip thì tràn xuống chip/lưới.
            .clipToBounds()
            .clickable(enabled = !viewState.autoplay, role = Role.Button, onClickLabel = stringResource(R.string.browse_play_preview_cd)) {
                onIntent(BrowseIntent.PlayPreview)
            },
    ) {
        if (slot != null) VideoSurface(slot = slot, modifier = Modifier.fillMaxSize(), shutter = art) else art()
        Box(Modifier.fillMaxSize().background(AppTheme.colors.scrimPreviewDock))
        Row(
            Modifier
                .statusBarsPadding()
                .padding(start = AppTheme.dimensions.s4, end = AppTheme.dimensions.s2, top = AppTheme.dimensions.s2)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.tab_browse), style = AppTypography.artTitle, color = media.text, modifier = Modifier.weight(1f))
            val pill = if (viewState.autoplay) R.string.browse_preview_auto else R.string.browse_preview_tap
            Text(
                text = stringResource(pill),
                style = AppTypography.badge,
                color = media.text,
                modifier = Modifier
                    .background(media.glassStrong, AppTheme.shapes.badge)
                    .border(1.dp, media.glassBorder, AppTheme.shapes.badge)
                    .padding(horizontal = 9.dp, vertical = AppTheme.dimensions.s1),
            )
            Box(
                Modifier
                    .size(AppTheme.dimensions.touchTarget)
                    .clip(AppTheme.shapes.full)
                    .clickable(role = Role.Button, onClickLabel = stringResource(R.string.search_title)) { onIntent(BrowseIntent.OpenSearch) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(AppIcons.Search, contentDescription = stringResource(R.string.search_title), tint = media.text, modifier = Modifier.size(21.dp))
            }
        }
        if (preview != null) {
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = AppTheme.dimensions.s4, end = AppTheme.dimensions.s4, bottom = 14.dp),
            ) {
                Text(
                    text = preview.title,
                    style = AppTypography.artTitle.copy(fontSize = 21.sp, lineHeight = 26.sp),
                    color = media.text,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val episodeCount = viewState.previewEpisodeTotal ?: preview.episodeCount
                val episodes = pluralStringResource(R.plurals.common_episode_count, episodeCount, episodeCount)
                Text(
                    // "97% match" của mockup bỏ: không có dữ liệu để tính.
                    text = listOfNotNull(preview.genre, episodes, preview.status.label()).joinToString(" · "),
                    style = AppTypography.caption.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 3.dp),
                )
                Row(Modifier.padding(top = 11.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    GradientButton(
                        text = stringResource(R.string.browse_play_ep_1),
                        onClick = { onIntent(BrowseIntent.PlayEpisode) },
                        modifier = Modifier.weight(1f),
                        shape = AppTheme.shapes.md,
                        minHeight = PLAY_BUTTON_HEIGHT,
                        leadingIcon = {
                            Icon(AppIcons.Play, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                        },
                    )
                    val saveLabel = stringResource(if (viewState.isSaved) R.string.common_saved_cd else R.string.common_save_cd)
                    Box(
                        Modifier
                            .size(PLAY_BUTTON_HEIGHT)
                            .clip(AppTheme.shapes.md)
                            .background(media.glass)
                            .border(1.dp, media.glassBorderSoft, AppTheme.shapes.md)
                            .clickable(role = Role.Button, onClickLabel = saveLabel) { onIntent(BrowseIntent.ToggleSave) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (viewState.isSaved) AppIcons.Check else AppIcons.Plus,
                            // onClickLabel đã đọc nhãn — đặt cả hai thì TalkBack đọc 2 lần.
                            contentDescription = null,
                            tint = media.text,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

/** Poster 2:3 r10; đang preview = viền gradient 2.5dp, còn lại mờ 72%. */
@Composable
private fun GridPoster(show: ShowCard, selected: Boolean, onClick: () -> Unit) {
    val shape = AppTheme.shapes.poster
    Box(
        Modifier
            .aspectRatio(2f / 3f)
            .clip(shape)
            .then(
                if (selected) {
                    Modifier.border(2.5.dp, AppTheme.colors.gradientCta, shape).padding(2.dp)
                } else {
                    Modifier.alpha(UNSELECTED_ALPHA)
                },
            )
            .selectable(selected = selected, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = show.title },
    ) {
        KeyArt(
            posterUrl = show.posterUrl,
            seed = show.id,
            modifier = Modifier
                .fillMaxSize()
                .clip(if (selected) AppTheme.shapes.sm else shape),
        )
    }
}

/** SCR-0601 của Browse: hàng chip + lưới 4 cột đúng chỗ poster sắp hiện. */
@Composable
private fun ColumnScope.BrowseGridSkeleton() {
    val gutter = AppTheme.dimensions.screenGutter
    SkeletonHost {
        Column(Modifier.weight(1f).padding(horizontal = gutter)) {
            Row(Modifier.padding(top = AppTheme.dimensions.s3), horizontalArrangement = Arrangement.spacedBy(AppTheme.dimensions.s2)) {
                repeat(4) { SkeletonBlock(Modifier.width(64.dp).height(34.dp), AppTheme.shapes.full) }
            }
            repeat(2) {
                Row(Modifier.padding(top = 30.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    repeat(GRID_COLUMNS) { SkeletonBlock(Modifier.weight(1f).aspectRatio(2f / 3f), AppTheme.shapes.poster) }
                }
            }
        }
    }
}

private const val GRID_COLUMNS = 4

/** Dock không chiếm quá 45% chiều cao (máy ngang). */
private const val MAX_DOCK_FRACTION = 0.45f
private const val LANDSCAPE_DOCK_FRACTION = 0.42f

/** Nút "Play Ep 1" và nút lưu của dock cao 42dp (mockup). */
private val PLAY_BUTTON_HEIGHT = 42.dp

@Preview(heightDp = 800)
@Composable
private fun BrowsePreview() {
    val show = ShowCard("12485", "My Wolf King Employer Is Super Clingy", "", 44, "Fantasy", ShowStatus.Completed)
    AppTheme {
        BrowseContent(
            viewState = BrowseViewState(
                isLoading = false,
                chips = listOf(BrowseChip(null, ""), BrowseChip("fantasy", "Fantasy")),
                grid = List(8) { show.copy(id = "$it") },
                preview = show.copy(id = "0"),
            ),
            slot = null,
            onIntent = {},
        )
    }
}
