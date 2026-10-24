package com.takehome.drama.feature.main.presentation.browse

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.takehome.drama.core.ui.foundation.error.UiError
import com.takehome.drama.core.ui.foundation.vm.SingleEvent
import com.takehome.drama.core.ui.foundation.vm.ViewIntent
import com.takehome.drama.core.ui.foundation.vm.ViewState
import com.takehome.drama.feature.main.presentation.common.ShowCard

/** [tagId] null = chip "All". */
@Immutable
data class BrowseChip(val tagId: String?, val label: String)

data class BrowseViewState(
    val isLoading: Boolean = true,
    val error: UiError? = null,
    val chips: List<BrowseChip> = emptyList(),
    val selectedChip: String? = null,
    val grid: List<ShowCard> = emptyList(),
    /** Show đang preview ở tầng trên — **không** đổi khi đổi chip. */
    val preview: ShowCard? = null,
    /** Số tập của file tập khi clip preview đã tải (lệch `catalogEpisodeCount`); null = dùng số catalogue. */
    val previewEpisodeTotal: Int? = null,
    val isSaved: Boolean = false,
    /** Preview tự chạy (Wi-Fi / tắt "Wi-Fi only"); false = pill "PREVIEW · tap", chạm để chạy. */
    val autoplay: Boolean = true,
) : ViewState

sealed interface BrowseIntent : ViewIntent {
    data class SelectChip(val tagId: String?) : BrowseIntent

    /** Chạm poster khác → đổi preview; chạm lại poster đang preview → Show detail. */
    data class SelectPoster(val showId: String) : BrowseIntent

    /** Chạm vùng preview khi không autoplay. */
    data object PlayPreview : BrowseIntent
    data object PlayEpisode : BrowseIntent
    data object ToggleSave : BrowseIntent
    data object OpenSearch : BrowseIntent
    data object Retry : BrowseIntent

    /** Tab ẩn vẫn RESUMED (pager giữ tab) → gate phát theo tab đang chọn. */
    data class TabVisible(val visible: Boolean) : BrowseIntent
    data object ScreenStarted : BrowseIntent
    /** Xoay máy cũng qua `ON_STOP` — bỏ qua để preview không nhả decoder rồi phát lại từ đầu. */
    data class ScreenStopped(val isChangingConfigurations: Boolean = false) : BrowseIntent
}

sealed interface BrowseSingleEvent : SingleEvent {
    data class OpenShow(val showId: String) : BrowseSingleEvent
    data class OpenPlayer(val showId: String, val episodeIndex: Int) : BrowseSingleEvent
    data object OpenSearch : BrowseSingleEvent
    data class ShowToast(@StringRes val messageRes: Int, val success: Boolean) : BrowseSingleEvent
}
