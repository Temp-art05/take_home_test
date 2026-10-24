package com.takehome.drama.feature.onboarding.presentation.taste

import androidx.compose.runtime.Immutable
import com.takehome.drama.core.ui.foundation.error.UiError
import com.takehome.drama.core.ui.foundation.vm.SingleEvent
import com.takehome.drama.core.ui.foundation.vm.ViewIntent
import com.takehome.drama.core.ui.foundation.vm.ViewState
import com.takehome.drama.domain.usecase.onboarding.Mood

/** Thẻ mood. [tagNames] = 2 tag đầu của cụm (theo ngôn ngữ nội dung) — phụ đề không hứa thứ catalogue không có. */
@Immutable
data class MoodItem(val mood: Mood, val tagNames: List<String>)

@Immutable
data class TasteChip(val id: String, val label: String)

data class TastePickerViewState(
    val isLoading: Boolean = true,
    val moods: List<MoodItem> = emptyList(),
    /** Mọi genre, thứ tự cố định theo catalog. */
    val chips: List<TasteChip> = emptyList(),
    val selectedMoods: Set<Mood> = emptySet(),
    val selectedTags: Set<String> = emptySet(),
    val chipsExpanded: Boolean = false,
    /** Chưa có catalogue (cài mới + offline): mood dựa trên tag thật nên không có gì để chọn — hiện 0603. */
    val error: UiError? = null,
) : ViewState {
    val visibleChips: List<TasteChip> get() = if (chipsExpanded) chips else chips.take(COLLAPSED_CHIPS)
    val hiddenChipCount: Int get() = chips.size - visibleChips.size
    /** Theo tag thật sẽ lưu: chọn mood rồi tắt tay hết chip của nó thì Continue = lưu gu rỗng = Skip. */
    val canContinue: Boolean get() = selectedTags.isNotEmpty()

    companion object {
        const val COLLAPSED_CHIPS = 6
    }
}

sealed interface TastePickerIntent : ViewIntent {
    data class ToggleMood(val mood: Mood) : TastePickerIntent
    data class ToggleTag(val tagId: String) : TastePickerIntent

    /** Chip "+ N more" mở rộng **tại chỗ** — không popup. */
    data object ExpandChips : TastePickerIntent

    data object Retry : TastePickerIntent

    /** [sdkInt] / [notificationsGranted] để quyết định có bước pre-prompt SCR-0005 hay không. */
    data class Continue(val sdkInt: Int, val notificationsGranted: Boolean) : TastePickerIntent
    data class Skip(val sdkInt: Int, val notificationsGranted: Boolean) : TastePickerIntent
}

sealed interface TastePickerSingleEvent : SingleEvent {
    /** `false` = onboarding đã xong ở đây (API < 33 hoặc đã có quyền) → Home. */
    data class Next(val showNotificationPrompt: Boolean) : TastePickerSingleEvent
}
