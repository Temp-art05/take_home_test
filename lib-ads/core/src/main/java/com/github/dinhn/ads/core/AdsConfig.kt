package com.github.dinhn.ads.core

/**
 * Cấu hình toàn cục cho SDK. Ad unit id KHÔNG nằm ở đây nữa — tầng app tự quản lý
 * theo từng placement (xem các *AdManager trong app). SDK chỉ giữ phần global.
 *
 * - [enabled] = false  -> toàn bộ SDK no-op (bản premium / tắt ad).
 * - [isShowLogAds]      -> giá trị khởi tạo cho log ad (tag "FOR_TEST_ADS").
 *   Đổi runtime qua [AdsSdk.setShowLogAds] (vd tap 7 lần ở splash).
 * - [remoteToggles]    -> bật/tắt theo từng key (mỗi placement có 2 key: high floor + all price).
 */
data class AdsConfig(
    val enabled: Boolean = true,
    val testMode: Boolean = false,
    val isShowLogAds: Boolean = false,
    /** [P3-2] Cho phép bật/tắt log runtime (vd tap 7 lần). false (production) ⇒ khoá, không toggle được. */
    val logToggleAllowed: Boolean = false,
    val testDeviceIds: List<String> = emptyList(),
    val networks: List<AdNetwork> = listOf(AdNetwork.ADMOB),
    val remoteToggles: Map<String, Boolean> = emptyMap(),
    /**
     * [P2-1/ILRD] true ⇒ lib TỰ forward doanh thu mỗi impression sang Firebase Analytics
     * (event `ad_impression`) — app KHÔNG phải tự viết mapping.
     * Yêu cầu: app thêm module `:ads-firebase` (dependency) + Firebase đã init (google-services
     * plugin + `google-services.json`). Thiếu module/Firebase ⇒ lib bỏ qua an toàn (log warning).
     * Core KHÔNG phụ thuộc Firebase (Rule 1) — forwarder (`FirebaseAdForwarder`, hiện thực
     * [OnPaidEvent]) được nạp qua reflection.
     */
    val trackWithFirebase: Boolean = false,
    /**
     * [P2-1] true ⇒ lib TỰ phân user-tier theo QUỐC GIA (bảng chuẩn [GeoTier]) lúc `init` rồi gọi
     * [AdsSdk.setUserTier] → bắn thêm `paid_ad_impression_<tier>` mỗi impression. Country lấy từ
     * device locale. Chỉ có tác dụng khi [trackWithFirebase] bật (event tier nằm trong forwarder).
     * Muốn thay bảng phân tier: đặt [geoTierTable]. Muốn tự quyết hoàn toàn: để false + tự gọi `setUserTier`.
     */
    val autoGeoTier: Boolean = false,
    /**
     * Bảng override cho [autoGeoTier]: ISO country (2 ký tự, HOA) → tên tier. **Rỗng = dùng bảng
     * chuẩn [GeoTier]**; non-empty = THAY hẳn bảng chuẩn. Country ngoài bảng ⇒ [GeoTier.DEFAULT_TIER].
     * Tên tier phải hợp lệ tên event Firebase (chữ/số/`_`, không dấu/space).
     */
    val geoTierTable: Map<String, String> = emptyMap(),
    /**
     * [P2-3] true ⇒ KHÔNG request ad cho tới khi UMP `canRequestAds()` = true (EEA/UK).
     * Mặc định false (opt-in) → không đổi hành vi. Bật thì app phải gọi [AdsSdk.requestConsent] sớm.
     */
    val requireConsentBeforeRequest: Boolean = false,
    /**
     * [P2-5] true ⇒ KHÔNG preload/show BẤT KỲ loại ad nào (inter/reward/appopen/banner/native) cho
     * tới khi tầng app áp xong remote config lần đầu và gọi [AdsSdk.setRemoteConfigReady] (vd sau
     * `fetchAndActivate()` hoặc dùng cache RC lần trước). Mặc định false (opt-in) → không đổi hành vi.
     * Bật ⇒ kill-switch remote luôn có hiệu lực TRƯỚC khi ad chạy, không lỡ show placement remote đã tắt.
     */
    val requireRemoteConfigBeforeRequest: Boolean = false,
    /** [P2-8 / M8] COPPA: true = app hướng tới trẻ em. null = không set. */
    val tagForChildDirectedTreatment: Boolean? = null,
    /** [P2-8 / M8] Người dùng dưới tuổi đồng ý (GDPR). null = không set. */
    val tagForUnderAgeOfConsent: Boolean? = null,
    /** [P2-8 / M8] Mức nội dung tối đa: "G" | "PG" | "T" | "MA". null = không set. */
    val maxAdContentRating: String? = null,
)
