package com.takehome.drama.feature.main.presentation.saved

import androidx.annotation.StringRes
import com.takehome.drama.core.ui.foundation.error.UiError
import com.takehome.drama.core.ui.foundation.vm.SingleEvent
import com.takehome.drama.core.ui.foundation.vm.ViewIntent
import com.takehome.drama.core.ui.foundation.vm.ViewState
import com.takehome.drama.domain.usecase.library.SavedFilter
import com.takehome.drama.feature.main.presentation.library.model.SavedRow

data class SavedViewState(
    val isLoading: Boolean = true,
    val error: UiError? = null,
    val filter: SavedFilter = SavedFilter.All,
    /** Đã lọc theo [filter]. */
    val items: List<SavedRow> = emptyList(),
    /** Số của chip "All · N". */
    val totalCount: Int = 0,
    val editing: Boolean = false,
    val menuFor: SavedRow? = null,
) : ViewState {
    /** Chưa lưu show nào → SCR-0602. Chip không khớp mà vẫn còn show đã lưu → empty nhỏ. */
    val isShelfEmpty: Boolean get() = !isLoading && error == null && totalCount == 0
}

sealed interface SavedIntent : ViewIntent {
    data object Back : SavedIntent
    data object Retry : SavedIntent
    data class SelectFilter(val filter: SavedFilter) : SavedIntent
    data object ToggleEdit : SavedIntent
    data class OpenRow(val showId: String) : SavedIntent
    data class OpenMenu(val row: SavedRow) : SavedIntent
    data object DismissMenu : SavedIntent
    data class Remove(val showId: String) : SavedIntent
    data object FindNext : SavedIntent
}

sealed interface SavedSingleEvent : SingleEvent {
    data object NavigateBack : SavedSingleEvent
    data class OpenDetail(val showId: String) : SavedSingleEvent
    data class ShowToast(@param:StringRes val messageRes: Int, val success: Boolean) : SavedSingleEvent
    data object GoBrowse : SavedSingleEvent
}
