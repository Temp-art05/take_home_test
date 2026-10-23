package com.takehome.drama.domain.repository

import com.takehome.drama.domain.model.ads.AdsConfig
import kotlinx.coroutines.flow.Flow

/** Cấu hình ads từ Remote Config (impl ở `:core:ads`, nơi thấy được Remote Config). */
interface AdsConfigRepository {
    /** Phát lại mỗi khi fetch mới về — fetch muộn sau Splash vẫn có hiệu lực. */
    val config: Flow<AdsConfig>

    /** Giá trị hiện có (fetch phiên này / phiên trước / default) — kiểm ngay trước khi show. */
    fun current(): AdsConfig
}
