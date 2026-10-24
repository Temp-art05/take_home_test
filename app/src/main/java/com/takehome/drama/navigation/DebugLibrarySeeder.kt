package com.takehome.drama.navigation

import com.takehome.drama.domain.model.WatchProgress
import com.takehome.drama.domain.repository.SavedShowRepository
import com.takehome.drama.domain.repository.WatchProgressRepository

/**
 * Chỉ flavor dev: dữ liệu Library giống mockup 4.1–4.3 để chụp so (`--ez debug_seed_library true`).
 * Idempotent — chạy lại chỉ ghi đè cùng bản ghi. `clear` xoá sạch (chụp trạng thái rỗng / SCR-0602).
 */
class DebugLibrarySeeder(
    private val watchProgress: WatchProgressRepository,
    private val savedShows: SavedShowRepository,
) {
    private data class Seed(val showId: String, val index: Int, val total: Int, val fraction: Double, val ageMs: Long)

    private val seeds = listOf(
        // Dở 62% hôm nay — "4 min left" (tập 10 phút).
        Seed("12485", index = 11, total = 44, fraction = 0.62, ageMs = 10 * MINUTE),
        Seed("13077", index = 12, total = 50, fraction = 0.19, ageMs = DAY),
        Seed("12818", index = 2, total = 45, fraction = 0.28, ageMs = 2 * DAY),
        // Tập cuối xem hết → Finished.
        Seed("12930", index = 49, total = 50, fraction = 1.0, ageMs = 3 * DAY),
        Seed("13069", index = 1, total = 30, fraction = 0.08, ageMs = 8 * DAY),
    )

    suspend fun seed(nowMillis: Long) {
        seeds.forEach { seed ->
            watchProgress.save(
                WatchProgress(
                    showId = seed.showId,
                    episodeIndex = seed.index,
                    episodeNumber = seed.index + 1,
                    episodeTotal = seed.total,
                    positionMs = (EPISODE_MS * seed.fraction).toLong(),
                    durationMs = EPISODE_MS,
                    updatedAt = nowMillis - seed.ageMs,
                ),
            )
        }
        // Lưu theo thứ tự ngược để show đầu danh sách là mới lưu nhất.
        SAVED.asReversed().forEach { savedShows.save(it) }
    }

    suspend fun clear() {
        seeds.forEach { watchProgress.removeShow(it.showId) }
        (SAVED + seeds.map { it.showId }).toSet().forEach { savedShows.remove(it) }
    }

    private companion object {
        const val MINUTE = 60_000L
        const val DAY = 24 * 60 * MINUTE
        const val EPISODE_MS = 10 * MINUTE
        val SAVED = listOf("13077", "12692", "12818", "13069", "12930")
    }
}
