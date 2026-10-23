package com.takehome.drama.feature.main.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.takehome.drama.core.designsystem.component.ErrorState
import com.takehome.drama.core.designsystem.component.LoadingState
import com.takehome.drama.core.designsystem.theme.AppTheme
import com.takehome.drama.core.designsystem.theme.AppTypography
import com.takehome.drama.core.ui.foundation.ext.CollectAsEffect
import com.takehome.drama.core.utils.toast.Toasty
import com.takehome.drama.domain.model.Post
import com.takehome.drama.feature.main.R
import com.takehome.drama.feature.main.presentation.home.component.PostItem
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = koinViewModel(),
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    viewModel.singleEvent.CollectAsEffect { event ->
        when (event) {
            is HomeSingleEvent.ShowError -> Toasty.error(context, event.messageRes, allowQueue = false).show()
            is HomeSingleEvent.BookmarkToggled -> Toasty.success(
                context,
                if (event.bookmarked) R.string.home_bookmarked else R.string.home_unbookmarked,
                allowQueue = false,
            ).show()
        }
    }

    HomeContent(
        viewState = viewState,
        onRetryClick = { viewModel.onRetryClick() },
        onLoadMoreClick = { viewModel.onLoadMoreClick() },
        onBookmarkClick = { post -> viewModel.onBookmarkClick(post) },
    )
}

@Composable
private fun HomeContent(
    viewState: HomeViewState,
    onRetryClick: () -> Unit,
    onLoadMoreClick: () -> Unit,
    onBookmarkClick: (Post) -> Unit,
) {
    when {
        viewState.isLoading -> LoadingState()

        viewState.errorRes != null -> ErrorState(
            message = stringResource(viewState.errorRes),
            retryLabel = stringResource(R.string.home_retry),
            onRetry = onRetryClick,
        )

        // LazyColumn dùng rememberLazyListState mặc định — tab được giữ sống nên scroll không mất khi đổi tab.
        else -> LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "title") {
                Text(
                    text = stringResource(R.string.tab_home),
                    style = AppTypography.headingH4Semibold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            items(viewState.posts, key = { it.id }) { post ->
                PostItem(
                    post = post,
                    bookmarked = post.id in viewState.bookmarkedIds,
                    onBookmarkClick = { onBookmarkClick(post) },
                )
            }
            if (viewState.hasMore) {
                item(key = "load_more") {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        if (viewState.isLoadingMore) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            OutlinedButton(onClick = onLoadMoreClick) {
                                Text(text = stringResource(R.string.home_load_more))
                            }
                        }
                    }
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun HomeContentPreview() {
    AppTheme {
        HomeContent(
            viewState = HomeViewState(
                posts = List(3) { Post(id = it, title = "Post title $it", body = "Post body $it") },
                hasMore = true,
                bookmarkedIds = setOf(1),
            ),
            onRetryClick = {},
            onLoadMoreClick = {},
            onBookmarkClick = {},
        )
    }
}
