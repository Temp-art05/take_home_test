package com.takehome.drama.feature.main.presentation.history

import androidx.annotation.StringRes
import com.takehome.drama.core.ui.foundation.error.UiError
import com.takehome.drama.core.ui.foundation.vm.SingleEvent
import com.takehome.drama.core.ui.foundation.vm.ViewIntent
import com.takehome.drama.core.ui.foundation.vm.ViewState
import com.takehome.drama.feature.main.presentation.library.model.HistoryRow

enum class HistoryTab { Watching, Finished }

data class HistoryViewState(
    val isLoading: Boolean = true,
    val error: UiError? = null,
    val tab: HistoryTab = HistoryTab.Watching,
    val watching: List<HistoryRow> = emptyList(),
    val finished: List<HistoryRow> = emptyList(),
    /** "Edit": ⋯ đổi thành ✕ xoá nhanh. Không lưu qua process death. */
    val editing: Boolean = false,
    /** Show đang mở sheet ⋯; null = đóng. */
    val menuFor: HistoryRow? = null,
) : ViewState {
    val rows: List<HistoryRow> get() = if (tab == HistoryTab.Watching) watching else finished
}

sealed interface HistoryIntent : ViewIntent {
    data object Back : HistoryIntent
    data object Retry : HistoryIntent
    data class SelectTab(val tab: HistoryTab) : HistoryIntent
    data object ToggleEdit : HistoryIntent
    data class OpenRow(val row: HistoryRow) : HistoryIntent
    data class OpenMenu(val row: HistoryRow) : HistoryIntent
    data object DismissMenu : HistoryIntent
    data class OpenDetails(val showId: String) : HistoryIntent
    data class Remove(val showId: String) : HistoryIntent
    /** CTA "Find your next flow" khi Watching rỗng → tab Browse. */
    data object FindNext : HistoryIntent
}

sealed interface HistorySingleEvent : SingleEvent {
    data object NavigateBack : HistorySingleEvent
    data class OpenPlayer(val showId: String, val episodeIndex: Int) : HistorySingleEvent
    data class OpenDetail(val showId: String) : HistorySingleEvent
    data class ShowToast(@param:StringRes val messageRes: Int, val success: Boolean) : HistorySingleEvent
    /** Tab Browse đã được yêu cầu qua `MainTabRequests` — Screen chỉ cần quay về Main. */
    data object GoBrowse : HistorySingleEvent
}
