package com.takehome.drama.feature.main.presentation.library

import com.takehome.drama.core.ui.foundation.error.UiError
import com.takehome.drama.core.ui.foundation.vm.SingleEvent
import com.takehome.drama.core.ui.foundation.vm.ViewIntent
import com.takehome.drama.core.ui.foundation.vm.ViewState
import com.takehome.drama.feature.main.presentation.library.model.HistoryRow

data class LibraryViewState(
    val isLoading: Boolean = true,
    val error: UiError? = null,
    val watchedCount: Int = 0,
    val savedCount: Int = 0,
    /** Tối đa 2 show đang xem dở, mới nhất trước. Rỗng → ẩn mục Recent. */
    val recent: List<HistoryRow> = emptyList(),
) : ViewState

sealed interface LibraryIntent : ViewIntent {
    data object Retry : LibraryIntent
    /** Hàng History và "See all ›" của Recent. */
    data object OpenHistory : LibraryIntent
    data object OpenSaved : LibraryIntent
    data class OpenRecent(val showId: String, val episodeIndex: Int) : LibraryIntent
}

sealed interface LibrarySingleEvent : SingleEvent {
    data object OpenHistory : LibrarySingleEvent
    data object OpenSaved : LibrarySingleEvent
    data class OpenPlayer(val showId: String, val episodeIndex: Int) : LibrarySingleEvent
}
