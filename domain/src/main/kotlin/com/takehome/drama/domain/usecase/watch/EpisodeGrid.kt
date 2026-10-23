package com.takehome.drama.domain.usecase.watch

/** Trạng thái một ô tập — cùng một "ngôn ngữ khoá" cho Detail, sheet chọn tập và Player. */
enum class EpisodeCellState { Locked, Playing, Watched, Unwatched }

data class EpisodeCell(
    /** `Episode.index` — khoá để mở tập, không phải vị trí trong list. */
    val index: Int,
    /** `Episode.displayNumber`. */
    val label: Int,
    val state: EpisodeCellState,
)

/** Một tab/chunk của lưới: `[start, endExclusive)` là **vị trí trong list**, nhãn theo số hiển thị. */
data class EpisodeRange(
    val start: Int,
    val endExclusive: Int,
    val firstLabel: Int,
    val lastLabel: Int,
)

/** Việc làm khi một tập phát hết. */
sealed interface EndAction {
    data class PlayNext(val index: Int) : EndAction

    /** Tập kế bị khoá — không tự phát vào. */
    data class NextLocked(val index: Int, val episodeNumber: Int) : EndAction

    /** User tắt autoplay — chỉ gợi ý tập kế. */
    data class NextAvailable(val index: Int, val episodeNumber: Int) : EndAction

    data object ShowFinished : EndAction
}
