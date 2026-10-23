package com.takehome.drama.feature.main.presentation.search

import androidx.annotation.StringRes
import com.takehome.drama.core.ui.foundation.vm.SingleEvent
import com.takehome.drama.core.ui.foundation.vm.ViewIntent
import com.takehome.drama.core.ui.foundation.vm.ViewState
import com.takehome.drama.feature.main.presentation.common.ShowCard

/** Ba trạng thái của design: chưa nhập, đang có kết quả, không kết quả. */
enum class SearchPhase { Idle, Results, NoResults }

data class SearchViewState(
    val query: String = "",
    val phase: SearchPhase = SearchPhase.Idle,
    val suggestions: List<String> = emptyList(),
    val matches: List<ShowCard> = emptyList(),
    /** Chip "Genres" lúc chưa nhập — chạm = điền tên genre vào ô. */
    val genres: List<String> = emptyList(),
    val popular: List<ShowCard> = emptyList(),
    @StringRes val errorRes: Int? = null,
) : ViewState

sealed interface SearchIntent : ViewIntent {
    data class QueryChanged(val query: String) : SearchIntent
    data object Clear : SearchIntent

    /** Chạm gợi ý / chip genre: điền vào ô và tìm ngay. */
    data class FillQuery(val text: String) : SearchIntent
    data class OpenShow(val showId: String) : SearchIntent
    data object Back : SearchIntent
    data object Retry : SearchIntent
}

sealed interface SearchSingleEvent : SingleEvent {
    data class OpenShow(val showId: String) : SearchSingleEvent
    data object NavigateBack : SearchSingleEvent
}
