package com.takehome.drama.feature.main.presentation.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.takehome.drama.core.designsystem.component.ChoiceChip
import com.takehome.drama.core.designsystem.component.EmptyState
import com.takehome.drama.core.designsystem.component.ErrorState
import com.takehome.drama.core.designsystem.component.KeyArt
import com.takehome.drama.core.designsystem.icon.AppIcons
import com.takehome.drama.core.designsystem.theme.AppTheme
import com.takehome.drama.core.designsystem.theme.AppTypography
import com.takehome.drama.core.navigation.core.NavigationRequest
import com.takehome.drama.core.navigation.destination.WatchRoutes
import com.takehome.drama.core.ui.foundation.ext.CollectAsEffect
import com.takehome.drama.domain.model.ShowStatus
import com.takehome.drama.feature.main.R
import com.takehome.drama.feature.main.presentation.common.ShowCard

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    watchRoutes: WatchRoutes,
    onNavigate: (NavigationRequest) -> Unit,
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    viewModel.singleEvent.CollectAsEffect { event ->
        when (event) {
            is SearchSingleEvent.OpenShow -> onNavigate(NavigationRequest.NavigateTo(watchRoutes.showDetail(event.showId)))
            SearchSingleEvent.NavigateBack -> onNavigate(NavigationRequest.PopBackStack)
        }
    }
    SearchContent(viewState = viewState, onIntent = viewModel::processIntent)
}

@Composable
private fun SearchContent(viewState: SearchViewState, onIntent: (SearchIntent) -> Unit) {
    val focusManager = LocalFocusManager.current
    // Ô nhập giữ text cục bộ (đồng bộ từng phím) — đi vòng qua StateFlow của VM thì gõ nhanh có thể
    // mất ký tự. Điền gợi ý / xoá đặt chữ ở đây với con trỏ cuối, rồi báo VM.
    var field by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(viewState.query, TextRange(viewState.query.length)))
    }
    val fill: (String) -> Unit = { text ->
        field = TextFieldValue(text, TextRange(text.length))
        onIntent(SearchIntent.FillQuery(text))
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        SearchHeader(
            field = field,
            onFieldChange = { value ->
                val changed = value.text != field.text
                field = value
                if (changed) onIntent(SearchIntent.QueryChanged(value.text))
            },
            onClear = {
                field = TextFieldValue()
                onIntent(SearchIntent.Clear)
            },
            onBack = { onIntent(SearchIntent.Back) },
        )
        when {
            viewState.errorRes != null -> ErrorState(
                message = stringResource(viewState.errorRes),
                retryLabel = stringResource(R.string.common_retry),
                onRetry = { onIntent(SearchIntent.Retry) },
            )
            viewState.phase == SearchPhase.NoResults -> EmptyState(
                message = stringResource(R.string.search_no_results, viewState.query.trim()),
            )
            else -> LazyColumn(contentPadding = PaddingValues(bottom = AppTheme.dimensions.s6)) {
                if (viewState.phase == SearchPhase.Results) {
                    items(viewState.suggestions, key = { "s-$it" }) { suggestion ->
                        SuggestionRow(
                            suggestion = suggestion,
                            query = viewState.query.trim(),
                            onClick = {
                                focusManager.clearFocus()
                                fill(suggestion)
                            },
                        )
                    }
                    item(key = "matches") { SectionTitle(stringResource(R.string.search_matches)) }
                    items(viewState.matches, key = { it.id }) { show ->
                        ShowRow(show, onClick = { onIntent(SearchIntent.OpenShow(show.id)) })
                    }
                } else {
                    item(key = "genres") { GenreChips(viewState.genres) { fill(it) } }
                    item(key = "popular") { SectionTitle(stringResource(R.string.search_popular)) }
                    items(viewState.popular, key = { it.id }) { show ->
                        ShowRow(show, onClick = { onIntent(SearchIntent.OpenShow(show.id)) })
                    }
                }
            }
        }
    }
}

/** `.sh-head` + `.sb.act`: back + ô tìm pill 44dp `surface2`, viền accent khi focus, nút xoá. Tự focus khi mở. */
@Composable
private fun SearchHeader(
    field: TextFieldValue,
    onFieldChange: (TextFieldValue) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    // Chỉ lần mở đầu: quay lại từ ShowDetail không bật bàn phím che kết quả.
    var autoFocused by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!autoFocused) {
            autoFocused = true
            focusRequester.requestFocus()
        }
    }
    val query = field.text
    Row(
        modifier = Modifier.padding(start = AppTheme.dimensions.s1, end = AppTheme.dimensions.s4, top = AppTheme.dimensions.s2, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(AppTheme.dimensions.touchTarget)
                .clip(AppTheme.shapes.full)
                .clickable(role = Role.Button, onClickLabel = stringResource(R.string.common_back), onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(AppIcons.Back, contentDescription = stringResource(R.string.common_back), tint = colors.onBackground, modifier = Modifier.size(22.dp))
        }
        val shape = AppTheme.shapes.full
        Row(
            modifier = Modifier
                .weight(1f)
                .height(AppTheme.dimensions.searchBar)
                .background(colors.surfaceVariant, shape)
                .then(if (focused) Modifier.border(1.dp, colors.primary, shape) else Modifier)
                .padding(start = AppTheme.dimensions.s4, end = AppTheme.dimensions.s1),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(AppIcons.Search, contentDescription = null, tint = if (focused) colors.onBackground else AppTheme.colors.text3, modifier = Modifier.size(18.dp))
            Box(Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(stringResource(R.string.search_hint), style = AppTypography.body.copy(fontSize = 14.5.sp), color = AppTheme.colors.text3, maxLines = 1)
                }
                BasicTextField(
                    value = field,
                    onValueChange = onFieldChange,
                    singleLine = true,
                    textStyle = AppTypography.body.copy(fontSize = 14.5.sp, color = colors.onBackground),
                    cursorBrush = SolidColor(colors.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onFocusChanged { focused = it.isFocused },
                )
            }
            if (query.isNotEmpty()) {
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(AppTheme.shapes.full)
                        .clickable(role = Role.Button, onClickLabel = stringResource(R.string.search_clear_cd), onClick = onClear),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(AppIcons.Close, contentDescription = stringResource(R.string.search_clear_cd), tint = AppTheme.colors.text3, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

/** `.sug li`: icon tìm + gợi ý (phần khớp đậm `text1`) + ↖ (điền gợi ý vào ô). */
@Composable
private fun SuggestionRow(suggestion: String, query: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val text = remember(suggestion, query, colors.onBackground) {
        buildAnnotatedString {
            val start = suggestion.indexOf(query, ignoreCase = true)
            if (query.isEmpty() || start < 0) {
                append(suggestion)
            } else {
                append(suggestion.substring(0, start))
                withStyle(SpanStyle(color = colors.onBackground, fontWeight = FontWeight.Bold)) {
                    append(suggestion.substring(start, start + query.length))
                }
                append(suggestion.substring(start + query.length))
            }
        }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = AppTheme.dimensions.s4, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.dimensions.s3),
    ) {
        Icon(AppIcons.Search, contentDescription = null, tint = AppTheme.colors.text3, modifier = Modifier.size(17.dp))
        Text(
            text = text,
            style = AppTypography.body.copy(fontSize = 14.5.sp),
            color = colors.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Icon(AppIcons.ArrowUpStart, contentDescription = null, tint = AppTheme.colors.text4, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = AppTypography.titleSmall,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(start = AppTheme.dimensions.s4, end = AppTheme.dimensions.s4, top = AppTheme.dimensions.s4, bottom = 10.dp),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GenreChips(genres: List<String>, onPick: (String) -> Unit) {
    if (genres.isEmpty()) return
    Column(Modifier.padding(horizontal = AppTheme.dimensions.s4)) {
        Text(
            text = stringResource(R.string.search_genres),
            style = AppTypography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = AppTheme.dimensions.s2, bottom = 10.dp),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(AppTheme.dimensions.s2), verticalArrangement = Arrangement.spacedBy(AppTheme.dimensions.s2)) {
            genres.forEach { genre -> ChoiceChip(text = genre, selected = false, onClick = { onPick(genre) }) }
        }
    }
}

/** `.li`: thumb 98×60, tên 14sp/600 một dòng, "Genre · N eps", ›. */
@Composable
private fun ShowRow(show: ShowCard, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = AppTheme.dimensions.s4, vertical = AppTheme.dimensions.s3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.dimensions.s3),
    ) {
        KeyArt(
            posterUrl = show.posterUrl,
            seed = show.id,
            modifier = Modifier
                .width(AppTheme.dimensions.listThumbWidth)
                .height(AppTheme.dimensions.listThumbHeight)
                .clip(AppTheme.shapes.sm),
        )
        Column(Modifier.weight(1f)) {
            Text(show.title, style = AppTypography.bodySemibold, color = colors.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val episodes = pluralStringResource(R.plurals.common_episode_count, show.episodeCount, show.episodeCount)
            Text(
                text = listOfNotNull(show.genre, episodes).joinToString(" · "),
                style = AppTypography.caption,
                color = AppTheme.colors.text3,
                maxLines = 1,
                modifier = Modifier.padding(top = AppTheme.dimensions.s1),
            )
        }
        Icon(AppIcons.ChevronEnd, contentDescription = null, tint = AppTheme.colors.text3, modifier = Modifier.size(18.dp))
    }
}

@Preview
@Composable
private fun SearchResultsPreview() {
    val show = ShowCard("1", "The Masked CEO", "", 74, "Romance", ShowStatus.Completed)
    AppTheme {
        SearchContent(
            viewState = SearchViewState(
                query = "ceo",
                phase = SearchPhase.Results,
                suggestions = listOf("CEO", "CEO in disguise"),
                matches = listOf(show, show.copy(id = "2", title = "The CEO's Contract Wife")),
            ),
            onIntent = {},
        )
    }
}
