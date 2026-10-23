package com.github.dinhn.ads.core

/**
 * Đo lường ad theo từng ad unit id ([M7/P2-9]) — nền để tối ưu.
 *
 * **show rate** (shown/loaded) là chỉ số rò rỉ doanh thu chính của app tự orchestrate
 * (load mà không show = lãng phí request + hạ chất lượng tài khoản).
 *
 * In-memory (process lifetime). Wire trong adapter — KHÔNG giữ ad object.
 */
object AdMetrics {

    private class Stat {
        var requests = 0L
        var loaded = 0L
        var failed = 0L
        var shown = 0L
        var impressions = 0L
        var totalLoadMs = 0L
        var loadSamples = 0L
    }

    private val stats = HashMap<String, Stat>()
    private fun s(adUnitId: String) = stats.getOrPut(adUnitId) { Stat() }

    fun onRequest(adUnitId: String) { s(adUnitId).requests++ }
    fun onLoaded(adUnitId: String, loadMs: Long) {
        val st = s(adUnitId); st.loaded++; st.totalLoadMs += loadMs; st.loadSamples++
    }
    fun onFailed(adUnitId: String) { s(adUnitId).failed++ }
    fun onShown(adUnitId: String) { s(adUnitId).shown++ }
    fun onImpression(adUnitId: String) { s(adUnitId).impressions++ }

    fun fillRate(adUnitId: String): Float {
        val st = stats[adUnitId] ?: return 0f
        return if (st.requests == 0L) 0f else st.loaded.toFloat() / st.requests
    }
    fun showRate(adUnitId: String): Float {
        val st = stats[adUnitId] ?: return 0f
        return if (st.loaded == 0L) 0f else st.shown.toFloat() / st.loaded
    }
    fun avgLoadMs(adUnitId: String): Long {
        val st = stats[adUnitId] ?: return 0L
        return if (st.loadSamples == 0L) 0L else st.totalLoadMs / st.loadSamples
    }

    /** Dump toàn bộ metric (gọi để log khi cần xem). */
    fun dump(): String = buildString {
        append("[AD_METRICS]")
        if (stats.isEmpty()) { append(" (empty)"); return@buildString }
        for ((id, st) in stats) {
            append("\n  id='").append(id).append("'")
            append(" req=").append(st.requests)
            append(" loaded=").append(st.loaded)
            append(" failed=").append(st.failed)
            append(" shown=").append(st.shown)
            append(" imp=").append(st.impressions)
            append(" fill=").append(pct(fillRate(id)))
            append(" show=").append(pct(showRate(id)))
            append(" avgLoadMs=").append(avgLoadMs(id))
        }
    }

    private fun pct(f: Float): String = "${(f * 100).toInt()}%"

    fun reset() { stats.clear() }
}
