package com.takehome.drama.domain.model

data class WatchProgress(
    val showId: String,
    val episodeIndex: Int,
    /** Lưu kèm để History hiện "Ep. 12 / 74" mà không phải tải file tập. */
    val episodeNumber: Int,
    /**
     * `index` tập cuối + 1 của **file tập** lúc lưu — không phải `catalogEpisodeCount` (13077 ghi 65,
     * file có 50: dùng số đó thì show không bao giờ "xong"). Show đang chiếu ra thêm tập thì
     * Finished cập nhật ở lần lưu kế.
     */
    val episodeTotal: Int,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAt: Long,
) {
    val remainingMs: Long get() = (durationMs - positionMs).coerceAtLeast(0)

    /** Coi là xem xong khi qua 95% hoặc còn dưới 3 giây (credit cuối tập). */
    val isEpisodeCompleted: Boolean
        get() = durationMs > 0 &&
            (positionMs >= durationMs * COMPLETED_RATIO || remainingMs < COMPLETED_TAIL_MS)

    /**
     * "Đã xem" trên lưới tập (ô xám, "N watched"): xem xong, hoặc đã xem ít nhất [WATCHED_MIN_MS] —
     * lướt qua khi vuốt pager không tính. Continue/History vẫn dựa vào [isEpisodeCompleted].
     */
    val isWatched: Boolean get() = isEpisodeCompleted || positionMs >= WATCHED_MIN_MS

    val isLastEpisode: Boolean get() = episodeIndex >= episodeTotal - 1

    private companion object {
        const val COMPLETED_RATIO = 0.95
        const val COMPLETED_TAIL_MS = 3_000L
        const val WATCHED_MIN_MS = 5_000L
    }
}

data class SavedShow(
    val showId: String,
    val savedAt: Long,
)
