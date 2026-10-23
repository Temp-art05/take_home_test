package com.takehome.drama.core.media

/** Mức preload mong muốn cho một tập — bản thuần của `DefaultPreloadManager.PreloadStatus`. */
sealed interface PreloadTarget {
    /** Tải sẵn [durationMs] đầu tiên (từ vị trí bắt đầu của tập). */
    data class Range(val durationMs: Long) : PreloadTarget

    data object TracksSelected : PreloadTarget
}

/**
 * [distance] = vị trí tập trừ vị trí tập đang phát. Tập đang phát (0) do player lo; tập khoá không
 * bao giờ preload (không phát được, tải là phí data).
 */
fun preloadTarget(distance: Int, isLocked: Boolean, profile: MediaProfile): PreloadTarget? {
    if (distance == 0 || isLocked) return null
    profile.preloadRangesMs[distance]?.let { return PreloadTarget.Range(it) }
    return if (distance in profile.tracksSelectedDistances) PreloadTarget.TracksSelected else null
}

/**
 * Vị trí các tập cần nằm trong preload manager khi đang phát [current]. Tập ngoài cửa sổ phải bị
 * remove — giữ lại là nguyên nhân chính gây OOM (blog Media3).
 */
fun preloadWindow(current: Int, size: Int, profile: MediaProfile, isPreloadable: (Int) -> Boolean): Set<Int> =
    profile.windowDistances
        .map { current + it }
        .filter { it in 0 until size && it != current && isPreloadable(it) }
        .toSet()

data class PreloadWindowDiff(val toAdd: Set<Int>, val toRemove: Set<Int>) {
    companion object {
        fun between(before: Set<Int>, after: Set<Int>) = PreloadWindowDiff(toAdd = after - before, toRemove = before - after)
    }
}
