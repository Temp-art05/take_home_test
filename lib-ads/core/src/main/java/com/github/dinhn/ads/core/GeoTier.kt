package com.github.dinhn.ads.core

/**
 * Phân **user-tier theo quốc gia** bằng một bảng "chuẩn chung" gom thị trường theo giá trị ad
 * (eCPM/ARPU) phổ biến trong UA. Dùng cho `paid_ad_impression_<tier>` khi bật [AdsConfig.autoGeoTier].
 *
 * ⚠️ KHÔNG có chuẩn chính thức duy nhất — đây là **mặc định có quan điểm**, đủ dùng cho đa số app.
 * App muốn khác: truyền [AdsConfig.geoTierTable] để thay hẳn, hoặc tự gọi [AdsSdk.setUserTier].
 *
 * Tên tier (`tier1/2/3`) đã hợp lệ làm tên event Firebase (`paid_ad_impression_tier1`…).
 */
object GeoTier {

    /** Tier cho country không nằm trong bảng. */
    const val DEFAULT_TIER = "tier3"

    /** Bảng chuẩn: ISO 3166 alpha-2 (HOA) → tier. tier1 = thị trường cao, tier2 = trung, còn lại tier3. */
    val DEFAULT_TABLE: Map<String, String> = buildMap {
        // tier1 — eCPM/ARPU cao
        listOf("US", "CA", "GB", "AU", "NZ", "DE", "FR", "JP", "NL", "SE", "NO", "DK",
            "CH", "IE", "AT", "BE", "FI", "SG", "KR", "AE", "LU", "HK").forEach { put(it, "tier1") }
        // tier2 — trung
        listOf("IT", "ES", "PT", "PL", "GR", "CZ", "HU", "SK", "IL", "SA", "QA", "KW",
            "TW", "MY", "TH", "TR", "RU", "MX", "BR", "AR", "CL", "ZA", "RO").forEach { put(it, "tier2") }
        // (còn lại → DEFAULT_TIER "tier3")
    }

    /**
     * Trả tier cho [country] (ISO 2 ký tự). [table] rỗng ⇒ dùng [DEFAULT_TABLE]; ngoài bảng ⇒ [DEFAULT_TIER].
     * [country] null/rỗng (không xác định được vùng) ⇒ [DEFAULT_TIER].
     */
    fun tierOf(country: String?, table: Map<String, String> = emptyMap()): String {
        val c = country?.trim()?.uppercase().orEmpty()
        if (c.isEmpty()) return DEFAULT_TIER
        val map = if (table.isEmpty()) DEFAULT_TABLE else table
        return map[c] ?: DEFAULT_TIER
    }
}
