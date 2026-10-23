package com.takehome.drama.feature.watch.presentation.detail

import androidx.annotation.StringRes
import com.takehome.drama.core.ui.foundation.vm.SingleEvent
import com.takehome.drama.core.ui.foundation.vm.ViewIntent
import com.takehome.drama.core.ui.foundation.vm.ViewState
import com.takehome.drama.domain.usecase.watch.EpisodeCell
import com.takehome.drama.domain.usecase.watch.EpisodeRange

data class ShowDetailViewState(
    val isLoading: Boolean = true,
    /** Lỗi toàn màn (catalogue lỗi / show không tồn tại). */
    @StringRes val errorRes: Int? = null,
    val title: String = "",
    val posterUrl: String = "",
    val tags: List<String> = emptyList(),
    /** Số tập theo file tập; trước khi file tải xong là số của catalogue. */
    val episodeTotal: Int = 0,
    /** Giữ `Long`; Screen format theo locale. */
    val viewCount: Long = 0,
    val synopsis: String = "",
    val synopsisExpanded: Boolean = false,
    val isEpisodesLoading: Boolean = true,
    /** File tập lỗi mà catalogue vẫn ổn: header vẫn hiện, lưới thay bằng lỗi inline. */
    @StringRes val episodesErrorRes: Int? = null,
    val cells: List<EpisodeCell> = emptyList(),
    val ranges: List<EpisodeRange> = emptyList(),
    /** Tab khoảng 20 tập đang xem; lưới chỉ hiện các tập của tab này. Mặc định tab chứa tập Continue. */
    val selectedRange: EpisodeRange? = null,
    /** Số tập đã xem (cho "44 eps · 7 watched"). */
    val watchedCount: Int = 0,
    val continueCta: ContinueCta? = null,
    val isSaved: Boolean = false,
) : ViewState {
    /** Ô của tab đang chọn; chưa có tab (≤ 20 tập) thì cả lưới. */
    val visibleCells: List<EpisodeCell>
        get() = selectedRange?.let { cells.subList(it.start.coerceAtMost(cells.size), it.endExclusive.coerceAtMost(cells.size)) } ?: cells
}

/** Nút "Continue Ep. N" / "Play Ep. N". */
data class ContinueCta(
    val episodeIndex: Int,
    val episodeNumber: Int,
    /** Đã xem show này → "Continue", chưa → "Play". */
    val isResume: Boolean,
    val isLocked: Boolean,
)

sealed interface ShowDetailIntent : ViewIntent {
    data object BackClick : ShowDetailIntent
    data object RetryClick : ShowDetailIntent

    /** Retry của lỗi inline ở lưới tập (header vẫn có). */
    data object RetryEpisodesClick : ShowDetailIntent
    data object ToggleSynopsis : ShowDetailIntent
    data object ContinueClick : ShowDetailIntent
    data class EpisodeClick(val index: Int) : ShowDetailIntent
    data class RangeSelect(val range: EpisodeRange) : ShowDetailIntent
    data object SaveClick : ShowDetailIntent
}

sealed interface ShowDetailSingleEvent : SingleEvent {
    data object NavigateBack : ShowDetailSingleEvent
    data class OpenPlayer(val showId: String, val episodeIndex: Int) : ShowDetailSingleEvent

    /** [formatArg] null = chuỗi không tham số. */
    data class ShowToast(@StringRes val messageRes: Int, val formatArg: Int?, val success: Boolean) : ShowDetailSingleEvent

    /** [position] là vị trí ô trong lưới. */
}
