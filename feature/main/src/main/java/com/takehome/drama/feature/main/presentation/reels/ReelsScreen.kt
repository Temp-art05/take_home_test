package com.takehome.drama.feature.main.presentation.reels

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.takehome.drama.core.designsystem.component.AppToastHost
import com.takehome.drama.core.designsystem.component.GhostPillButton
import com.takehome.drama.core.designsystem.component.GradientButton
import com.takehome.drama.core.designsystem.component.KeyArt
import com.takehome.drama.core.designsystem.component.rememberToastState
import com.takehome.drama.core.designsystem.ext.clickableNoRipple
import com.takehome.drama.core.designsystem.icon.AppIcons
import com.takehome.drama.core.designsystem.theme.AppTheme
import com.takehome.drama.core.designsystem.theme.AppTypography
import com.takehome.drama.core.media.KeepScreenOnWhilePlaying
import com.takehome.drama.core.media.PlaybackProgress
import com.takehome.drama.core.media.PlaybackSpeed
import com.takehome.drama.core.media.PlaybackState
import com.takehome.drama.core.media.VideoSlot
import com.takehome.drama.core.media.VideoSurface
import com.takehome.drama.core.navigation.core.NavigationRequest
import com.takehome.drama.core.navigation.destination.WatchRoutes
import com.takehome.drama.core.ui.foundation.ext.CollectAsEffect
import com.takehome.drama.core.ui.foundation.format.formatCompactCount
import com.takehome.drama.core.utils.extension.findActivity
import com.takehome.drama.feature.main.R
import com.takehome.drama.feature.main.presentation.common.UiErrorState
import kotlin.math.abs
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

/** Overlay mờ hết khi trang lệch ~60% chiều cao (như Player). */
private const val OVERLAY_FADE = 1.6f

/** Tự sang reel kế: trượt chậm, mềm (như Player tự sang tập). */
private const val AUTO_SCROLL_MS = 650

@Composable
internal fun ReelsScreen(
    watchRoutes: WatchRoutes,
    onNavigate: (NavigationRequest) -> Unit,
    isSelected: Boolean,
    viewModel: ReelsViewModel = koinViewModel(),
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    // Không `by`: chỉ thanh track đọc `.value` → tick 500ms không recompose cả màn.
    val progress = viewModel.playbackProgress.collectAsStateWithLifecycle()
    val slots by viewModel.player.slots.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(initialPage = viewState.current) { viewState.pages.size }
    val scope = rememberCoroutineScope()
    val toastState = rememberToastState()
    val context = LocalContext.current
    val resources = LocalResources.current

    viewModel.singleEvent.CollectAsEffect { event ->
        when (event) {
            is ReelsSingleEvent.ScrollToPage -> scope.launch {
                pagerState.animateScrollToPage(event.page, animationSpec = tween(AUTO_SCROLL_MS, easing = FastOutSlowInEasing))
            }
            is ReelsSingleEvent.OpenPlayer ->
                onNavigate(NavigationRequest.NavigateTo(watchRoutes.player(event.showId, event.episodeIndex)))
            is ReelsSingleEvent.ShowToast -> toastState.show(resources.getString(event.messageRes), success = event.success)
        }
    }
    LaunchedEffect(isSelected) { viewModel.processIntent(ReelsIntent.TabVisible(isSelected)) }
    LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.processIntent(ReelsIntent.ScreenStarted) }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        val changingConfigurations = context.findActivity()?.isChangingConfigurations == true
        viewModel.processIntent(ReelsIntent.ScreenStopped(changingConfigurations))
    }
    if (isSelected) KeepScreenOnWhilePlaying(viewModel.player)

    val hasPages = viewState.pages.isNotEmpty()
    LaunchedEffect(pagerState, hasPages) {
        if (!hasPages) return@LaunchedEffect
        // VM là nguồn sự thật (sau xoay máy / process death nhảy thẳng tới reel đang xem).
        if (pagerState.currentPage != viewState.current) pagerState.scrollToPage(viewState.current)
        snapshotFlow { pagerState.settledPage }.collect { viewModel.processIntent(ReelsIntent.PageSettled(it)) }
    }

    Box(Modifier.fillMaxSize().background(AppTheme.colors.media.background)) {
        ReelsContent(viewState, pagerState, progress, slots, viewModel::processIntent)
        // 0605: nổi ngay trên tab bar (tab bar nằm dưới nội dung tab), không giữa màn che video.
        AppToastHost(state = toastState, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = AppTheme.dimensions.s4))
    }
}

@Composable
private fun ReelsContent(
    viewState: ReelsViewState,
    pagerState: PagerState,
    progress: State<PlaybackProgress>,
    slots: Map<Int, VideoSlot>,
    onIntent: (ReelsIntent) -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        if (viewState.pages.isNotEmpty()) {
            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                // Trang ±1 dựng sẵn để surface của player lân cận đã có khung hình khi vuốt nhẹ.
                beyondViewportPageCount = 1,
                key = { viewState.pages[it].showId },
            ) { page ->
                val isCurrent = page == viewState.current
                // Chỉ trang hiện tại nhận playback/expanded: buffering, playWhenReady đổi không recompose trang ±1.
                ReelPageView(
                    page = viewState.pages[page],
                    slot = slots[page],
                    isCurrent = isCurrent,
                    playback = if (isCurrent) viewState.playback else null,
                    speed = viewState.speed,
                    descriptionExpanded = isCurrent && viewState.descriptionExpanded,
                    // Theo id reel, không theo `current`: khung đầu sau khi lật, player còn báo vị trí reel trước.
                    progress = { progress.value.fractionFor(viewState.pages[page].showId) },
                    overlayAlpha = { (1f - abs(pagerState.getOffsetDistanceInPages(page)) * OVERLAY_FADE).coerceIn(0f, 1f) },
                    onIntent = onIntent,
                )
            }
        }
        // Scrim trên cố định cho status bar (mockup `.sc-t` 150).
        Box(Modifier.fillMaxWidth().height(150.dp).background(AppTheme.colors.scrimTop))
        when {
            viewState.error != null -> UiErrorState(
                error = viewState.error,
                onRetry = { onIntent(ReelsIntent.Retry) },
                modifier = Modifier.background(MaterialTheme.colorScheme.background),
            )
            viewState.isLoading -> Spinner(Modifier.align(Alignment.Center))
        }
    }
}

/**
 * Một trang = một show, **tự chứa** video + scrim + rail + mô tả + nút + track: vuốt là cả trang trượt,
 * overlay mờ dần ([overlayAlpha], đọc trong `graphicsLayer` → không recompose mỗi frame). Trang lân cận
 * vẽ surface của player đã prepare + pause; chưa có khung hình thì key art.
 */
@Composable
private fun ReelPageView(
    page: ReelPage,
    slot: VideoSlot?,
    isCurrent: Boolean,
    /** null với trang không phải trang hiện tại. */
    playback: PlaybackState?,
    speed: Float,
    descriptionExpanded: Boolean,
    progress: () -> Float,
    overlayAlpha: () -> Float,
    onIntent: (ReelsIntent) -> Unit,
) {
    val art: @Composable () -> Unit = {
        KeyArt(posterUrl = page.posterUrl, seed = page.showId, modifier = Modifier.fillMaxSize())
    }
    Box(Modifier.fillMaxSize()) {
        if (slot != null) VideoSurface(slot = slot, modifier = Modifier.fillMaxSize(), shutter = art) else art()
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(0.46f).background(AppTheme.colors.scrimBottom))
        val ready = page.status == ClipStatus.Ready
        if (playback != null && ready) {
            val label = stringResource(if (playback.playWhenReady) R.string.reels_pause_cd else R.string.reels_play_cd)
            Box(
                Modifier
                    .fillMaxSize()
                    .clickableNoRipple(onClickLabel = label, role = Role.Button) { onIntent(ReelsIntent.TogglePlay) }
                    .semantics { contentDescription = label },
            )
        }
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = overlayAlpha() }) {
            when {
                page.status == ClipStatus.Loading -> Spinner(Modifier.align(Alignment.Center))
                page.status == ClipStatus.Failed && isCurrent -> CenterAction(
                    title = stringResource(R.string.reels_error_clip),
                    action = stringResource(R.string.common_retry),
                    onAction = { onIntent(ReelsIntent.RetryClip) },
                )
                page.status == ClipStatus.Unavailable -> CenterAction(title = stringResource(R.string.reels_unavailable))
                playback != null && playback.error != null -> CenterAction(
                    title = stringResource(R.string.reels_error_playback),
                    action = stringResource(R.string.common_retry),
                    onAction = { onIntent(ReelsIntent.RetryClip) },
                )
                playback != null && ready && playback.isBuffering && playback.playWhenReady -> Spinner(Modifier.align(Alignment.Center))
                playback != null && ready && !playback.playWhenReady -> PlayBadge(Modifier.align(Alignment.Center))
            }
            ReelOverlay(
                page = page,
                isCurrent = isCurrent,
                speed = speed,
                expanded = descriptionExpanded,
                progress = progress,
                onIntent = onIntent,
            )
        }
    }
}

@Composable
private fun BoxScope.ReelOverlay(
    page: ReelPage,
    isCurrent: Boolean,
    speed: Float,
    expanded: Boolean,
    progress: () -> Float,
    onIntent: (ReelsIntent) -> Unit,
) {
    val media = AppTheme.colors.media
    Column(
        Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .padding(start = AppTheme.dimensions.s4, end = AppTheme.dimensions.s3, bottom = 18.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f).padding(end = AppTheme.dimensions.s4)) {
                Text(
                    text = page.episodeNumber?.let { stringResource(R.string.reels_title_episode, page.title, it) } ?: page.title,
                    style = shadowed(AppTypography.titleSmall.copy(fontSize = 16.sp)),
                    color = media.text,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                ReelDescription(page.description, expanded, onToggle = { if (isCurrent) onIntent(ReelsIntent.ToggleDescription) })
                val total = page.episodeTotal
                if (total != null) {
                    GradientButton(
                        text = pluralStringResource(R.plurals.reels_watch_all, total, total),
                        onClick = { if (isCurrent) onIntent(ReelsIntent.WatchAll) },
                        modifier = Modifier.padding(top = AppTheme.dimensions.s3).height(40.dp),
                        leadingIcon = {
                            Icon(AppIcons.Play, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                        },
                    )
                }
            }
            // Luôn giữ chỗ rail (ẩn + không bấm được khi clip chưa Ready): cột mô tả không đổi bề rộng,
            // chữ không wrap nhảy lúc clip tải xong.
            val ready = page.status == ClipStatus.Ready
            ReelRail(
                page = page,
                speed = speed,
                enabled = ready,
                onLike = { if (isCurrent) onIntent(ReelsIntent.ToggleLike) },
                onSpeed = { if (isCurrent) onIntent(ReelsIntent.CycleSpeed) },
                onSave = { if (isCurrent) onIntent(ReelsIntent.ToggleSave) },
                modifier = Modifier
                    .padding(bottom = AppTheme.dimensions.s5)
                    .alpha(if (ready) 1f else 0f),
            )
        }
        ReelTrack(progress = progress, modifier = Modifier.padding(top = 18.dp))
    }
}

/** 2 dòng + "… more" nằm cuối dòng 2 khi bị cắt; mở rộng thì hiện đủ + "less" ở dòng riêng. */
@Composable
private fun ReelDescription(description: String, expanded: Boolean, onToggle: () -> Unit) {
    if (description.isBlank()) return
    val media = AppTheme.colors.media
    val style = shadowed(AppTypography.label.copy(fontWeight = FontWeight.Normal, lineHeight = 19.5.sp))
    val moreStyle = style.copy(color = media.textFaint, fontWeight = FontWeight.SemiBold)
    val more = stringResource(R.string.reels_more)
    // Độ dài phần mô tả giữ lại ở dòng 2; null = chưa đo hoặc không bị cắt.
    var keep by remember(description) { mutableStateOf<Int?>(null) }
    val collapsedText = remember(description, keep, more) {
        keep?.let { n ->
            buildAnnotatedString {
                append(description.take(n).trimEnd())
                append("… ")
                withStyle(SpanStyle(color = media.textFaint, fontWeight = FontWeight.SemiBold)) { append(more) }
            }
        }
    }
    Text(
        text = if (expanded) AnnotatedString(description) else collapsedText ?: AnnotatedString(description),
        style = style,
        color = media.textBody,
        maxLines = if (expanded) EXPANDED_LINES else COLLAPSED_LINES,
        overflow = if (expanded || collapsedText != null) TextOverflow.Clip else TextOverflow.Ellipsis,
        onTextLayout = { layout ->
            if (!expanded && keep == null && layout.hasVisualOverflow) {
                val lineEnd = layout.getLineEnd(COLLAPSED_LINES - 1, visibleEnd = true)
                keep = (lineEnd - more.length - MORE_RESERVE_CHARS).coerceAtLeast(1)
            }
        },
        // Mô tả ngắn (không tràn) không bấm được — tránh hiện "less" vô nghĩa.
        modifier = Modifier
            .padding(top = 6.dp)
            .clickableNoRipple(enabled = keep != null || expanded, onClick = onToggle),
    )
    if (expanded) {
        Text(
            text = stringResource(R.string.reels_less),
            style = moreStyle,
            modifier = Modifier.clickableNoRipple(onClick = onToggle),
        )
    }
}

/** `.rail` của Reels: tim + số like, "1×" (chạm = 1 → 1.5 → 2), Save (lưu show). */
@Composable
private fun ReelRail(
    page: ReelPage,
    speed: Float,
    enabled: Boolean,
    onLike: () -> Unit,
    onSpeed: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val media = AppTheme.colors.media
    Column(modifier, verticalArrangement = Arrangement.spacedBy(AppTheme.dimensions.s5), horizontalAlignment = Alignment.CenterHorizontally) {
        RailItem(label = formatCompactCount(page.likeCount, Locale.current.platformLocale), contentDescription = stringResource(R.string.reels_like_cd), enabled = enabled, onClick = onLike) {
            Icon(
                imageVector = if (page.isLiked) AppIcons.HeartFilled else AppIcons.Heart,
                contentDescription = null,
                tint = if (page.isLiked) MaterialTheme.colorScheme.primary else media.text,
                modifier = Modifier.size(AppTheme.dimensions.railIcon),
            )
        }
        RailItem(label = stringResource(R.string.reels_speed), contentDescription = stringResource(R.string.reels_speed_cd, PlaybackSpeed.label(speed)), enabled = enabled, onClick = onSpeed) {
            Text(
                text = stringResource(R.string.reels_speed_value, PlaybackSpeed.label(speed)),
                style = shadowed(AppTypography.titleSmall.copy(fontWeight = FontWeight.ExtraBold, lineHeight = 29.sp)),
                color = media.text,
                modifier = Modifier.height(AppTheme.dimensions.railIcon),
            )
        }
        val saveLabel = stringResource(if (page.isSaved) R.string.common_saved_cd else R.string.common_save_cd)
        RailItem(label = stringResource(R.string.reels_save), contentDescription = saveLabel, enabled = enabled, onClick = onSave) {
            Icon(
                imageVector = if (page.isSaved) AppIcons.BookmarkFilled else AppIcons.Bookmark,
                contentDescription = null,
                tint = if (page.isSaved) MaterialTheme.colorScheme.primary else media.text,
                modifier = Modifier.size(AppTheme.dimensions.railIcon),
            )
        }
    }
}

@Composable
private fun RailItem(label: String, contentDescription: String, enabled: Boolean, onClick: () -> Unit, icon: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .defaultMinSize(minWidth = AppTheme.dimensions.touchTarget, minHeight = AppTheme.dimensions.touchTarget)
            .clip(AppTheme.shapes.md)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        icon()
        Text(text = label, style = shadowed(AppTypography.captionSemibold.copy(fontSize = 12.sp)), color = AppTheme.colors.media.text)
    }
}

/** Track 3dp gradient, chỉ hiển thị (vào Player để tua). Vẽ ở pha draw: tick vị trí không recompose. */
@Composable
private fun ReelTrack(progress: () -> Float, modifier: Modifier = Modifier) {
    val track = AppTheme.colors.media.track
    val gradient = AppTheme.colors.gradientCta
    Box(
        modifier
            .fillMaxWidth()
            .height(3.dp)
            .drawBehind {
                val radius = CornerRadius(size.height / 2)
                drawRoundRect(track, cornerRadius = radius)
                val fraction = progress()
                if (fraction > 0f) drawRoundRect(gradient, size = size.copy(width = size.width * fraction), cornerRadius = radius)
            },
    )
}

@Composable
private fun Spinner(modifier: Modifier = Modifier) {
    CircularProgressIndicator(
        color = AppTheme.colors.media.text,
        trackColor = AppTheme.colors.media.spinnerTrack,
        strokeWidth = 2.5.dp,
        modifier = modifier.size(44.dp),
    )
}

@Composable
private fun PlayBadge(modifier: Modifier = Modifier) {
    Box(modifier.size(64.dp).background(AppTheme.colors.media.glass, AppTheme.shapes.full), contentAlignment = Alignment.Center) {
        Icon(AppIcons.Play, contentDescription = null, tint = AppTheme.colors.media.text, modifier = Modifier.size(32.dp))
    }
}

@Composable
private fun BoxScope.CenterAction(title: String, action: String? = null, onAction: () -> Unit = {}) {
    Column(Modifier.align(Alignment.Center).padding(horizontal = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = AppTypography.titleSmall, color = AppTheme.colors.media.text, textAlign = TextAlign.Center)
        if (action != null) GhostPillButton(text = action, onClick = onAction, modifier = Modifier.padding(top = AppTheme.dimensions.s4))
    }
}

@Composable
private fun shadowed(style: TextStyle): TextStyle =
    style.copy(shadow = Shadow(color = AppTheme.colors.media.textShadow, blurRadius = 6f))

private const val COLLAPSED_LINES = 2
// Chừa chỗ cho "… " và sai số đo chữ để "more" không đẩy xuống dòng 3.
private const val MORE_RESERVE_CHARS = 3
private const val EXPANDED_LINES = 8

@Preview(widthDp = 360, heightDp = 760)
@Composable
private fun ReelsPreview() {
    AppTheme {
        ReelsContent(
            viewState = ReelsViewState(
                isLoading = false,
                pages = listOf(
                    ReelPage(
                        showId = "12692",
                        title = "The Ex-Wife Returns",
                        description = "Three years on, she walks into her ex-husband's wedding…",
                        posterUrl = "",
                        status = ClipStatus.Ready,
                        episodeIndex = 0,
                        episodeNumber = 1,
                        episodeTotal = 68,
                        likeCount = 24_100,
                    ),
                ),
            ),
            pagerState = rememberPagerState { 1 },
            progress = remember { mutableStateOf(PlaybackProgress(itemId = "12692", positionMs = 30_000, durationMs = 100_000)) },
            slots = emptyMap(),
            onIntent = {},
        )
    }
}
