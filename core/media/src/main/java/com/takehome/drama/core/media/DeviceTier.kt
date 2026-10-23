package com.takehome.drama.core.media

import android.app.ActivityManager
import android.content.Context

/** Mức máy quyết định preload bao nhiêu, buffer bao lớn (xem [MediaProfile]). */
enum class DeviceTier { NORMAL, LOW }

/**
 * Máy yếu khi hệ thống tự báo low-RAM, heap app ≤ 192 MB, hoặc tổng RAM < 3 GB — ngưỡng cố định để
 * hành vi preload đoán trước được, không phụ thuộc lúc chạy.
 */
fun classifyDeviceTier(isLowRam: Boolean, memoryClassMb: Int, totalMemBytes: Long): DeviceTier = when {
    isLowRam -> DeviceTier.LOW
    memoryClassMb <= LOW_MEMORY_CLASS_MB -> DeviceTier.LOW
    totalMemBytes < LOW_TOTAL_MEM_BYTES -> DeviceTier.LOW
    else -> DeviceTier.NORMAL
}

/** [forceLow] chỉ để kiểm/chụp ở flavor dev (ép chính sách máy yếu trên máy khoẻ). */
class DeviceTierDetector(private val context: Context, private val forceLow: () -> Boolean = { false }) {

    fun detect(): DeviceTier {
        if (forceLow()) return DeviceTier.LOW
        val activityManager = context.getSystemService(ActivityManager::class.java)
        val memoryInfo = ActivityManager.MemoryInfo().also(activityManager::getMemoryInfo)
        return classifyDeviceTier(
            isLowRam = activityManager.isLowRamDevice,
            memoryClassMb = activityManager.memoryClass,
            totalMemBytes = memoryInfo.totalMem,
        )
    }
}

private const val LOW_MEMORY_CLASS_MB = 192
private const val LOW_TOTAL_MEM_BYTES = 3L * 1024 * 1024 * 1024
