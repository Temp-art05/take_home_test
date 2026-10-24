package com.takehome.drama.core.ads

import android.app.Application
import androidx.compose.ui.graphics.toArgb
import com.github.dinhn.ads.core.AdsSdk
import com.takehome.drama.core.ads.config.AdsConfigParser
import com.takehome.drama.core.analytics.remoteconfig.DEFAULT_AWAIT_MS
import com.takehome.drama.core.analytics.remoteconfig.RemoteConfigSource
import com.takehome.drama.core.designsystem.theme.AppColors
import com.takehome.drama.domain.config.AppConfig
import com.takehome.drama.domain.model.ads.AdPlacement
import com.takehome.drama.domain.model.ads.AdsConfig
import com.takehome.drama.domain.repository.AdsConfigRepository
import com.takehome.drama.domain.usecase.ads.AdsDecision
import com.takehome.drama.domain.usecase.ads.AdsPolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import com.github.dinhn.ads.core.AdsConfig as LibAdsConfig

/**
 * Khởi động lib ads ở `Application.onCreate` theo cách của HomeMorph: init (MobileAds khởi tạo nền nhờ
 * meta-data `OPTIMIZE_INITIALIZATION`), log sự kiện, đăng ký placement, rồi đẩy policy Remote Config vào
 * toggle của lib.
 *
 * Lib bật `requireRemoteConfigBeforeRequest`: không request ad nào cho tới khi config lần đầu được áp —
 * tức chờ fetch xong hoặc hết [DEFAULT_AWAIT_MS] (cùng ngưỡng Splash), để placement đã tắt trên Console
 * không kịp hiện trước khi fetch về. Sau đó vẫn quan sát: fetch về muộn cũng áp ngay.
 */
class AdsInitializer(
    private val app: Application,
    private val appConfig: AppConfig,
    private val remoteConfig: RemoteConfigSource,
    private val adsConfigRepository: AdsConfigRepository,
    private val appOpen: AppOpenAdController,
) {
    // Lib dùng HashMap + Compose state, chỉ an toàn trên main thread.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var interstitialIntervalSec = AdsConfig.DEFAULT_INTERSTITIAL_INTERVAL_SEC

    fun start() {
        val started = runCatching {
            AdsSdk.init(
                app,
                LibAdsConfig(
                    isShowLogAds = appConfig.isDebug,
                    // Tap logo Splash 7 lần (AdsLogToggle) — release còn cần RC `enable_log_ads`.
                    logToggleAllowed = true,
                    remoteToggles = AdPlacement.entries.associate { it.id to it.defaultEnabled },
                    requireRemoteConfigBeforeRequest = true,
                    // Lib tự bắn ad_impression / paid_ad_impression[_tier] / revenue milestone sang Firebase.
                    trackWithFirebase = true,
                    autoGeoTier = true,
                ),
            )
            AdsSdk.setOnAdEvent(AdEventLog::onEvent)
            AdsSdk.setOnPaidEvent(AdEventLog::onRevenue)
            AdPlacement.entries.forEach(::register)
            appOpen.start()
        }.onFailure { Timber.tag(AdsConfigParser.TAG).e(it, "Ads init failed - running without ads") }
        if (started.isFailure) return

        scope.launch {
            adsConfigRepository.config.collect { config ->
                val toggles = remoteToggles(config, appConfig.versionName)
                Timber.tag(AdsConfigParser.TAG).d("toggles %s (version %s)", toggles, appConfig.versionName)
                runCatching {
                    AdsSdk.setRemoteToggles(toggles)
                    applyInterstitialInterval(config.intervalBetweenInterstitialSec)
                }
            }
        }
        scope.launch {
            withTimeoutOrNull(DEFAULT_AWAIT_MS) { remoteConfig.isReady.first { it } }
            // Toggle đã được collector trên đẩy từ giá trị phiên trước / default trước lúc này.
            runCatching { AdsSdk.setRemoteConfigReady() }
            appOpen.onRemoteConfigReady()
        }
    }

    /**
     * Lib chép min-interval vào placement lúc đăng ký, nên đổi default thôi không đủ (lỗi của HomeMorph:
     * `interval_between_interstitial` không có tác dụng) — đăng ký lại interstitial với giá trị mới.
     */
    private fun applyInterstitialInterval(seconds: Long) {
        if (seconds == interstitialIntervalSec) return
        interstitialIntervalSec = seconds
        AdsSdk.setDefaultInterstitialMinIntervalMs(seconds * 1_000)
        register(AdPlacement.InterstitialOnboarding)
    }

    /**
     * Cả hai toggle key = placement id: key lạ mặc định `true` trong lib, nên tách key sẽ làm placement không
     * tắt được. High floor để trống (lib lọc id rỗng) — bài test chỉ có một test id mỗi format.
     */
    private fun register(placement: AdPlacement) {
        val id = AdUnits.of(placement)
        val key = placement.id
        when (placement) {
            AdPlacement.InterstitialOnboarding -> AdsSdk.registerInterstitial(
                name = key, highFloorId = "", normalId = id, enableHighFloorKey = key, enableNormalKey = key,
                minLoadingMs = 0L, minIntervalMs = interstitialIntervalSec * 1_000, maxPerSession = 1,
            )
            AdPlacement.RewardedBonus -> AdsSdk.registerRewarded(
                name = key, highFloorId = "", normalId = id, enableHighFloorKey = key, enableNormalKey = key,
                minLoadingMs = 0L,
            )
            AdPlacement.NativeHome -> AdsSdk.registerNative(
                name = key, highFloorId = "", normalId = id, enableHighFloorKey = key, enableNormalKey = key,
            )
            // Nền che sau ad lúc resume = nền app (lib mặc định trắng, nháy trên theme tối).
            AdPlacement.AppOpenResume -> AdsSdk.registerAppOpen(
                name = key, highFloorId = "", normalId = id, enableHighFloorKey = key, enableNormalKey = key,
                screenGuardColor = AppColors.Dark.bg.toArgb(),
            )
        }
    }
}

/** Toggle đẩy vào lib: key = `placement.id` (khớp key đã đăng ký ở `register`), value = policy D9. */
internal fun remoteToggles(config: AdsConfig, versionName: String): Map<String, Boolean> =
    AdPlacement.entries.associate { placement ->
        placement.id to (AdsPolicy.decide(config, versionName, placement) == AdsDecision.Enabled)
    }
