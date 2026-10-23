package com.takehome.drama.data.util

import com.takehome.drama.domain.usecase.discovery.ClockTime
import com.takehome.drama.domain.usecase.discovery.WallClock
import java.util.Calendar

/**
 * Giờ máy theo múi giờ hiện tại. [overrideHour] chỉ để flavor dev chụp Home ở một khung giờ cố định
 * (`--ei debug_hour 21`); bản prod không bao giờ gán.
 */
class SystemWallClock : WallClock {

    @Volatile
    var overrideHour: Int? = null

    override fun now(): ClockTime {
        val calendar = Calendar.getInstance()
        val hour = overrideHour ?: calendar.get(Calendar.HOUR_OF_DAY)
        // Giờ ép thì phút cố định như mockup (21:45) để ảnh chụp ổn định.
        val minute = if (overrideHour != null) DEBUG_MINUTE else calendar.get(Calendar.MINUTE)
        return ClockTime(hour, minute)
    }

    private companion object {
        const val DEBUG_MINUTE = 45
    }
}
