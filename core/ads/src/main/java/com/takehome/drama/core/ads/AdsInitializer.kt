package com.takehome.drama.core.ads

import android.app.Application
import com.github.dinhn.ads.core.AdsSdk
import com.takehome.drama.core.ads.config.AdsConfigParser
import com.takehome.drama.core.analytics.remoteconfig.DEFAULT_AWAIT_MS
import com.takehome.drama.core.analytics.remoteconfig.RemoteConfigSource
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
 * Khởi động lib ads ở `Application.onCreate`: init (MobileAds chạy nền, không chặn Splash), đăng ký 3
 * placement, rồi đẩy policy Remote Config vào toggle của lib.
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
) {
    // Lib dùng HashMap + Compose state, chỉ an toàn trên main thread.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    fun start() {
        val started = runCatching {
            AdsSdk.init(
                app,
                LibAdsConfig(
                    isShowLogAds = appConfig.isDebug,
                    requireRemoteConfigBeforeRequest = true,
                ),
            )
            AdPlacement.entries.forEach(::register)
        }.onFailure { Timber.tag(AdsConfigParser.TAG).e(it, "Ads init failed - running without ads") }
        if (started.isFailure) return

        scope.launch {
            adsConfigRepository.config.collect { config ->
                val toggles = remoteToggles(config, appConfig.versionName)
                Timber.tag(AdsConfigParser.TAG).d("toggles %s (version %s)", toggles, appConfig.versionName)
                runCatching { AdsSdk.setRemoteToggles(toggles) }
            }
        }
        scope.launch {
            withTimeoutOrNull(DEFAULT_AWAIT_MS) { remoteConfig.isReady.first { it } }
            // Toggle đã được collector trên đẩy từ giá trị phiên trước / default trước lúc này.
            runCatching { AdsSdk.setRemoteConfigReady() }
        }
    }

    /**
     * Cả hai toggle key = placement id: key lạ mặc định `true` trong lib, nên tách key sẽ làm placement không
     * tắt được. High floor để trống (lib lọc id rỗng) — bài test chỉ có một test id mỗi format.
     * `showLoading = false`: không dùng overlay đen của lib (D6) — app chỉ show ad đã preload sẵn.
     */
    private fun register(placement: AdPlacement) {
        val id = AdUnits.of(placement)
        val key = placement.id
        when (placement) {
            AdPlacement.InterstitialOnboarding -> AdsSdk.registerInterstitial(
                name = key, highFloorId = "", normalId = id, enableHighFloorKey = key, enableNormalKey = key,
                minLoadingMs = 0L, maxPerSession = 1, showLoading = false,
            )
            AdPlacement.RewardedBonus -> AdsSdk.registerRewarded(
                name = key, highFloorId = "", normalId = id, enableHighFloorKey = key, enableNormalKey = key,
                minLoadingMs = 0L, showLoading = false,
            )
            AdPlacement.NativeHome -> AdsSdk.registerNative(
                name = key, highFloorId = "", normalId = id, enableHighFloorKey = key, enableNormalKey = key,
            )
        }
    }
}

/** Toggle đẩy vào lib: key = `placement.id` (khớp key đã đăng ký ở `register`), value = policy D9. */
internal fun remoteToggles(config: AdsConfig, versionName: String): Map<String, Boolean> =
    AdPlacement.entries.associate { placement ->
        placement.id to (AdsPolicy.decide(config, versionName, placement) == AdsDecision.Enabled)
    }
