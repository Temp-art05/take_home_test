package com.github.dinhn.ads.core

/**
 * Rule 3 — mỗi ad key có một công tắc Boolean, DEFAULT = true.
 * key = false  => không request + ẩn ad (chặn ngay ở gate).
 * Phase 2: nạp giá trị từ Firebase Remote Config qua [setAll].
 */
class AdRemoteToggle(initial: Map<String, Boolean> = emptyMap()) {

    private val map = HashMap(initial)

    /** Default true nếu key chưa khai báo. */
    fun isEnabled(key: String): Boolean = map[key] ?: true

    fun set(key: String, enabled: Boolean) { map[key] = enabled }

    fun setAll(values: Map<String, Boolean>) { map.putAll(values) }

    fun snapshot(): Map<String, Boolean> = map.toMap()
}
