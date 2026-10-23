package com.takehome.drama.feature.main.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.takehome.drama.core.ads.AdsPresenter
import com.takehome.drama.core.designsystem.component.ErrorState
import com.takehome.drama.core.designsystem.component.KeyArt
import com.takehome.drama.core.designsystem.component.Wordmark
import com.takehome.drama.core.designsystem.icon.AppIcons
import com.takehome.drama.core.designsystem.theme.AppTheme
import com.takehome.drama.core.designsystem.theme.AppTypography
import com.takehome.drama.core.navigation.core.NavigationRequest
import com.takehome.drama.core.navigation.destination.WatchRoutes
import com.takehome.drama.core.ui.foundation.ext.CollectAsEffect
import com.takehome.drama.core.ui.foundation.format.format
import com.takehome.drama.core.ui.foundation.format.rememberTimeFormat
import com.takehome.drama.domain.model.ShowStatus
import com.takehome.drama.domain.model.ads.AdPlacement
import com.takehome.drama.domain.usecase.discovery.ClockTime
import com.takehome.drama.domain.usecase.discovery.Daypart
import com.takehome.drama.domain.usecase.discovery.opensAt
import com.takehome.drama.feature.main.R
import com.takehome.drama.feature.main.navigation.SearchScreenProvider
import com.takehome.drama.feature.main.presentation.common.ShowCard
import com.takehome.drama.feature.main.presentation.common.alsoFlowingRes
import com.takehome.drama.feature.main.presentation.common.label
import com.takehome.drama.feature.main.presentation.common.moodRes
import com.takehome.drama.feature.main.presentation.common.pickRes
import com.takehome.drama.feature.main.presentation.common.pitchRes
import com.takehome.drama.feature.main.presentation.common.titleRes
import com.takehome.drama.feature.main.presentation.main.model.MainTab
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
internal fun HomeScreen(
    watchRoutes: WatchRoutes,
    onNavigate: (NavigationRequest) -> Unit,
    onSwitchTab: (MainTab) -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
    adsPresenter: AdsPresenter = koinInject(),
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    viewModel.singleEvent.CollectAsEffect { event ->
        when (event) {
            is HomeSingleEvent.OpenShow -> onNavigate(NavigationRequest.NavigateTo(watchRoutes.showDetail(event.showId)))
            is HomeSingleEvent.OpenPlayer ->
                onNavigate(NavigationRequest.NavigateTo(watchRoutes.player(event.showId, event.episodeIndex)))
            HomeSingleEvent.OpenSearch -> onNavigate(NavigationRequest.NavigateTo(SearchScreenProvider.Search()))
            HomeSingleEvent.OpenBrowse -> onSwitchTab(MainTab.Browse)
            HomeSingleEvent.OpenSettings -> onSwitchTab(MainTab.Settings)
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.processIntent(HomeIntent.ScreenStarted) }
    HomeContent(
        viewState = viewState,
        onIntent = viewModel::processIntent,
        nativeAd = { modifier -> adsPresenter.NativeAdSlot(AdPlacement.NativeHome, modifier) },
    )
}

@Composable
private fun HomeContent(
    viewState: HomeViewState,
    onIntent: (HomeIntent) -> Unit,
    nativeAd: @Composable (Modifier) -> Unit = {},
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        HomeAppBar(onSearch = { onIntent(HomeIntent.OpenSearch) }, onBell = { onIntent(HomeIntent.OpenBell) })
        when {
            viewState.errorRes != null -> ErrorState(
                message = stringResource(viewState.errorRes),
                retryLabel = stringResource(R.string.common_retry),
                onRetry = { onIntent(HomeIntent.Retry) },
            )
            else -> HomeFeedList(viewState, onIntent, nativeAd)
        }
    }
}

@Composable
private fun HomeAppBar(onSearch: () -> Unit, onBell: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = AppTheme.dimensions.s4, end = AppTheme.dimensions.s2, top = AppTheme.dimensions.s2, bottom = AppTheme.dimensions.s1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Wordmark(modifier = Modifier.weight(1f))
        AppBarIcon(AppIcons.Search, stringResource(R.string.search_title), onSearch)
        AppBarIcon(AppIcons.Bell, stringResource(R.string.home_notifications_cd), onBell)
    }
}

@Composable
private fun AppBarIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(AppTheme.dimensions.touchTarget)
            .clip(AppTheme.shapes.full)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(21.dp))
    }
}

@Composable
private fun HomeFeedList(viewState: HomeViewState, onIntent: (HomeIntent) -> Unit, nativeAd: @Composable (Modifier) -> Unit) {
    val gutter = AppTheme.dimensions.screenGutter
    LazyColumn(contentPadding = PaddingValues(bottom = AppTheme.dimensions.s6)) {
        item(key = "daypart") { DaypartHeader(viewState.daypart, viewState.clock) }
        if (viewState.isLoading) {
            item(key = "loading") { HomeSkeleton() }
            return@LazyColumn
        }
        viewState.hero?.let { hero ->
            item(key = "hero") {
                HeroCard(hero, viewState.daypart, onClick = { onIntent(HomeIntent.OpenShow(hero.id)) })
            }
        }
        item(key = "upcoming") {
            Row(
                modifier = Modifier
                    .padding(start = gutter, end = gutter, top = AppTheme.dimensions.s3)
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                viewState.upcoming.forEach { DaypartCard(it, Modifier.weight(1f)) }
            }
        }
        if (viewState.continueRow.isNotEmpty()) {
            item(key = "continue-head") { RowHeader(stringResource(R.string.home_pick_up)) }
            item(key = "continue") {
                LazyRow(contentPadding = PaddingValues(horizontal = gutter), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(viewState.continueRow, key = { it.show.id }) { card ->
                        ContinueCardView(card, onClick = { onIntent(HomeIntent.ContinueShow(card.show.id, card.episodeIndex)) })
                    }
                }
            }
        }
        // native_home: sau phần "đang dở" của user, trước các rail khám phá — hoà vào feed, không chen giữa
        // hero và thẻ daypart. Không có ad (tắt / chưa load / offline) thì ô không vẽ gì, không khung trống.
        if (viewState.showNativeAd) {
            item(key = "ad-native") {
                nativeAd(Modifier.padding(start = gutter, end = gutter, top = AppTheme.dimensions.s5))
            }
        }
        viewState.rails.forEach { rail ->
            item(key = "rail-head-${rail.title}") { RowHeader(rail.title) }
            item(key = "rail-${rail.title}") { PosterRail(rail.shows) { onIntent(HomeIntent.OpenShow(it)) } }
        }
        if (viewState.alsoFlowing.isNotEmpty()) {
            item(key = "also-head") {
                RowHeader(
                    title = stringResource(viewState.daypart.alsoFlowingRes),
                    action = stringResource(R.string.home_see_all),
                    onAction = { onIntent(HomeIntent.SeeAll) },
                )
            }
            item(key = "also") { PosterRail(viewState.alsoFlowing) { onIntent(HomeIntent.OpenShow(it)) } }
        }
    }
}

@Composable
private fun DaypartHeader(daypart: Daypart, clock: ClockTime) {
    val timeFormat = rememberTimeFormat()
    Column(Modifier.padding(start = AppTheme.dimensions.s4, end = AppTheme.dimensions.s4, top = 6.dp)) {
        Text(stringResource(daypart.titleRes), style = AppTypography.daypart, color = MaterialTheme.colorScheme.onBackground)
        Text(
            text = stringResource(R.string.home_daypart_line, clock.format(timeFormat), stringResource(daypart.moodRes)),
            style = AppTypography.caption.copy(fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold),
            color = AppTheme.colors.text3,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}

@Composable
private fun HeroCard(hero: ShowCard, daypart: Daypart, onClick: () -> Unit) {
    val shape = AppTheme.shapes.hero
    Box(
        Modifier
            .padding(start = AppTheme.dimensions.s4, end = AppTheme.dimensions.s4, top = AppTheme.dimensions.s3)
            .fillMaxWidth()
            .height(AppTheme.dimensions.homeHero)
            .clip(shape)
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        KeyArt(posterUrl = hero.posterUrl, seed = hero.id, modifier = Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(AppTheme.colors.scrimHero))
        GradientBadge(
            text = stringResource(daypart.pickRes),
            modifier = Modifier.padding(AppTheme.dimensions.s3),
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(start = AppTheme.dimensions.s4, end = AppTheme.dimensions.s4, bottom = 14.dp),
        ) {
            Text(hero.title, style = AppTypography.artTitle, color = AppTheme.colors.media.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                text = heroMeta(hero),
                style = AppTypography.caption.copy(fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
    }
}

/** "Binge-ready · 44 eps complete · Fantasy"; show chưa xong không gọi là binge-ready. */
@Composable
private fun heroMeta(hero: ShowCard): String {
    val episodes = pluralStringResource(R.plurals.common_episode_count, hero.episodeCount, hero.episodeCount)
    val parts = buildList {
        if (hero.status == ShowStatus.Completed) {
            add(stringResource(R.string.home_binge_ready))
            add(stringResource(R.string.home_eps_complete, episodes))
        } else {
            add(episodes)
            add(hero.status.label())
        }
        hero.genre?.let(::add)
    }
    return parts.joinToString(" · ")
}

@Composable
internal fun GradientBadge(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = AppTypography.badge,
        color = MaterialTheme.colorScheme.onPrimary,
        modifier = modifier
            .background(AppTheme.colors.gradientCta, AppTheme.shapes.badge)
            .padding(horizontal = 9.dp, vertical = 5.dp),
    )
}

@Composable
private fun DaypartCard(daypart: Daypart, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val shape = AppTheme.shapes.card
    val timeFormat = rememberTimeFormat()
    Column(
        modifier
            .fillMaxHeight()
            .background(colors.surface, shape)
            .border(1.dp, colors.outline, shape)
            .padding(horizontal = AppTheme.dimensions.s3, vertical = 11.dp),
    ) {
        Text(
            text = stringResource(R.string.home_daypart_card, daypart.opensAt.format(timeFormat), stringResource(daypart.titleRes)).uppercase(),
            style = AppTypography.badge,
            color = AppTheme.colors.text3,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = stringResource(daypart.pitchRes),
            style = AppTypography.caption.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Bold),
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(top = AppTheme.dimensions.s1),
        )
    }
}

@Composable
private fun RowHeader(title: String, action: String? = null, onAction: () -> Unit = {}) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = AppTheme.dimensions.s4, end = AppTheme.dimensions.s4, top = AppTheme.dimensions.s4, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = AppTypography.titleSmall.copy(fontSize = 15.5.sp, fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        if (action != null) {
            // Chevron là icon autoMirror (lật theo RTL) thay vì ký tự "›" cứng trong string.
            Row(
                modifier = Modifier
                    .clip(AppTheme.shapes.sm)
                    .clickable(role = Role.Button, onClick = onAction)
                    .padding(AppTheme.dimensions.s2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = action,
                    style = AppTypography.caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                    color = AppTheme.colors.text3,
                )
                Icon(AppIcons.ChevronEnd, contentDescription = null, tint = AppTheme.colors.text3, modifier = Modifier.size(12.dp))
            }
        }
    }
}

@Composable
private fun PosterRail(shows: List<ShowCard>, onOpen: (String) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = AppTheme.dimensions.screenGutter),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        items(shows, key = { it.id }) { show ->
            KeyArt(
                posterUrl = show.posterUrl,
                seed = show.id,
                modifier = Modifier
                    .width(AppTheme.dimensions.railPosterWidth)
                    .height(AppTheme.dimensions.railPosterHeight)
                    .clip(AppTheme.shapes.posterRail)
                    .clickable(role = Role.Button) { onOpen(show.id) }
                    // Poster không có chữ: TalkBack đọc tên show thay vì chỉ "Button".
                    .semantics { contentDescription = show.title },
            )
        }
    }
}

/** `.cw`: thumbnail 158×92 + thanh tiến độ gradient + tên + "Ep. N". */
@Composable
private fun ContinueCardView(card: ContinueCard, onClick: () -> Unit) {
    Column(
        Modifier
            .width(158.dp)
            .clip(AppTheme.shapes.sm)
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Box(Modifier.fillMaxWidth().height(92.dp).clip(AppTheme.shapes.poster)) {
            KeyArt(posterUrl = card.show.posterUrl, seed = card.show.id, modifier = Modifier.fillMaxSize())
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(AppTheme.colors.media.spinnerTrack),
            ) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(card.progress).background(AppTheme.colors.gradientCta))
            }
        }
        Text(
            text = card.show.title,
            style = AppTypography.labelSemibold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 7.dp),
        )
        Text(
            text = stringResource(R.string.common_episode_number, card.episodeNumber),
            style = AppTypography.caption.copy(fontSize = 11.sp),
            color = AppTheme.colors.text3,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}

/** Khung tải theo đúng bố cục (0601 làm kỹ ở Phase 6). */
@Composable
private fun HomeSkeleton() {
    val surface = MaterialTheme.colorScheme.surface
    Column(Modifier.padding(horizontal = AppTheme.dimensions.s4)) {
        Box(Modifier.padding(top = AppTheme.dimensions.s3).fillMaxWidth().height(AppTheme.dimensions.homeHero).background(surface, AppTheme.shapes.hero))
        Row(Modifier.padding(top = AppTheme.dimensions.s3), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            repeat(2) { Box(Modifier.weight(1f).height(64.dp).background(surface, AppTheme.shapes.card)) }
        }
        Row(Modifier.padding(top = 42.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            repeat(3) {
                Box(Modifier.width(AppTheme.dimensions.railPosterWidth).height(AppTheme.dimensions.railPosterHeight).background(surface, AppTheme.shapes.posterRail))
            }
        }
    }
}

@Preview(heightDp = 800)
@Composable
private fun HomeContentPreview() {
    val show = ShowCard("12485", "My Wolf King Employer Is Super Clingy", "", 44, "Fantasy", ShowStatus.Completed)
    AppTheme {
        HomeContent(
            viewState = HomeViewState(
                isLoading = false,
                daypart = Daypart.Night,
                clock = ClockTime(21, 45),
                upcoming = listOf(Daypart.Morning, Daypart.Lunch),
                hero = show,
                alsoFlowing = listOf(show.copy(id = "a"), show.copy(id = "b"), show.copy(id = "c")),
            ),
            onIntent = {},
        )
    }
}
