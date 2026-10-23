package com.takehome.drama.feature.main.presentation.home

import androidx.lifecycle.viewModelScope
import com.takehome.drama.core.ui.foundation.error.toErrorRes
import com.takehome.drama.core.ui.foundation.vm.BaseViewModel
import com.takehome.drama.domain.model.Post
import com.takehome.drama.domain.usecase.post.PostFeedUseCase
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * SAMPLE — trọn vòng dữ liệu: hàm public → use case → repository → Ktor/Room → state → UI.
 * ViewModel chỉ map kết quả sang state/event; trang kế, khử trùng lặp, `hasMore` là của use case.
 */
class HomeViewModel(
    private val postFeed: PostFeedUseCase,
) : BaseViewModel<HomeViewState, HomeSingleEvent>() {

    init {
        loadFirstPage()
        observeBookmarks()
    }

    override fun initState() = HomeViewState()

    fun onRetryClick() {
        loadFirstPage()
    }

    fun onLoadMoreClick() {
        if (currentState.isLoadingMore || !currentState.hasMore) return
        updateState { copy(isLoadingMore = true) }
        viewModelScope.launch {
            postFeed.nextPage(loaded = currentState.posts)
                .onSuccess { page ->
                    updateState { copy(isLoadingMore = false, posts = posts + page.items, hasMore = page.hasMore) }
                }
                .onFailure { throwable ->
                    Timber.e(throwable, "Load more posts failed")
                    updateState { copy(isLoadingMore = false) }
                    sendEvent(HomeSingleEvent.ShowError(throwable.toErrorRes()))
                }
        }
    }

    fun onBookmarkClick(post: Post) {
        val willBookmark = post.id !in currentState.bookmarkedIds
        viewModelScope.launch {
            postFeed.toggleBookmark(post)
                .onSuccess { sendEvent(HomeSingleEvent.BookmarkToggled(bookmarked = willBookmark)) }
                .onFailure { throwable ->
                    Timber.e(throwable, "Toggle bookmark failed")
                    sendEvent(HomeSingleEvent.ShowError(throwable.toErrorRes()))
                }
        }
    }

    private fun loadFirstPage() {
        updateState { copy(isLoading = true, errorRes = null) }
        viewModelScope.launch {
            postFeed.firstPage()
                .onSuccess { page ->
                    updateState { copy(isLoading = false, posts = page.items, hasMore = page.hasMore) }
                }
                .onFailure { throwable ->
                    Timber.e(throwable, "Load posts failed")
                    updateState { copy(isLoading = false, errorRes = throwable.toErrorRes()) }
                }
        }
    }

    /** Room là nguồn sự thật cho bookmark: ghi xong stream tự đẩy state mới về. */
    private fun observeBookmarks() {
        viewModelScope.launch {
            postFeed.observeBookmarkedIds().collect { ids -> updateState { copy(bookmarkedIds = ids) } }
        }
    }
}
