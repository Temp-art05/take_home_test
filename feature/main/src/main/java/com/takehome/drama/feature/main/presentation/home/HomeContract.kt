package com.takehome.drama.feature.main.presentation.home

import androidx.annotation.StringRes
import com.takehome.drama.core.ui.foundation.vm.SingleEvent
import com.takehome.drama.core.ui.foundation.vm.ViewState
import com.takehome.drama.domain.model.Post

/**
 * SAMPLE — loading là field của state; lỗi mang `@StringRes` (đổi ngôn ngữ thì text đổi theo).
 */
data class HomeViewState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val posts: List<Post> = emptyList(),
    val hasMore: Boolean = false,
    val bookmarkedIds: Set<Int> = emptySet(),
    @param:StringRes val errorRes: Int? = null,
) : ViewState

sealed interface HomeSingleEvent : SingleEvent {
    data class ShowError(@param:StringRes val messageRes: Int) : HomeSingleEvent
    data class BookmarkToggled(val bookmarked: Boolean) : HomeSingleEvent
}
