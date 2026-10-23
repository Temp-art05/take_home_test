package com.github.dinhn.ads.admob

import android.app.Activity
import android.content.Context
import android.view.View
import com.github.dinhn.ads.core.AdEvent
import com.github.dinhn.ads.core.AdEventType
import com.github.dinhn.ads.core.AdHandle
import com.github.dinhn.ads.core.AdLoadResult
import com.github.dinhn.ads.core.AdMetrics
import com.github.dinhn.ads.core.AdNetwork
import com.github.dinhn.ads.core.AdRevenue
import com.github.dinhn.ads.core.AdTracker
import com.github.dinhn.ads.core.AdValue
import com.github.dinhn.ads.core.AdsAdapter
import com.github.dinhn.ads.core.AdsLogger
import com.github.dinhn.ads.core.AdsSdk
import com.github.dinhn.ads.core.BannerAdCallback
import com.github.dinhn.ads.core.FullScreenAdCallback
import com.github.dinhn.ads.core.FullScreenAdState
import com.github.dinhn.ads.core.RewardAdCallback
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdValue as GmaAdValue
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.OnPaidEventListener
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.ResponseInfo
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/**
 * Adapter AdMob — mô hình handle + callback. KHÔNG giữ state ad nội bộ:
 * mỗi load trả về [AdHandle] để tầng app giữ -> hỗ trợ nhiều placement song song.
 * Instance hoá qua reflection bởi AdsSdk (constructor rỗng).
 */
class AdMobAdapter : AdsAdapter {

    override val network: AdNetwork = AdNetwork.ADMOB

    override fun initialize(
        context: Context,
        testMode: Boolean,
        testDeviceIds: List<String>,
        onInitialized: () -> Unit,
    ) {
        // [M8/P2-8] testDeviceIds + COPPA/child + maxAdContentRating từ AdsConfig.
        val cfg = AdsSdk.config
        val builder = RequestConfiguration.Builder()
        if (testDeviceIds.isNotEmpty()) builder.setTestDeviceIds(testDeviceIds)
        cfg.tagForChildDirectedTreatment?.let {
            builder.setTagForChildDirectedTreatment(
                if (it) RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE
                else RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE,
            )
        }
        cfg.tagForUnderAgeOfConsent?.let {
            builder.setTagForUnderAgeOfConsent(
                if (it) RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE
                else RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE,
            )
        }
        cfg.maxAdContentRating?.let { builder.setMaxAdContentRating(it) }
        MobileAds.setRequestConfiguration(builder.build())
        // FlowDrama: initialize trên thread nền (khuyến nghị của Google) — gọi trên main trong
        // Application.onCreate làm chậm frame đầu của Splash. Callback SDK tự về main thread.
        Thread({ MobileAds.initialize(context) { onInitialized() } }, "ads-init").start()
    }

    private fun request(): AdRequest = AdRequest.Builder().build()

    /** Mô tả lỗi load đầy đủ: code + domain + message + cause + responseInfo (để gỡ "Internal error"). */
    private fun describe(e: LoadAdError): String = buildString {
        append("code=").append(e.code)
        append(" domain=").append(e.domain)
        append(" msg=").append(e.message)
        e.cause?.let { c ->
            append(" cause=[code=").append(c.code)
                .append(" domain=").append(c.domain)
                .append(" msg=").append(c.message).append("]")
        }
        e.responseInfo?.let { append(" responseInfo=").append(it) }
    }

    /** Map paid event của AdMob → [AdValue] core và phát qua [AdRevenue] (P2-1 / ILRD). */
    private fun emitPaid(format: String, adUnitId: String, v: GmaAdValue, responseInfo: ResponseInfo?) {
        AdRevenue.emit(
            AdValue(
                valueMicros = v.valueMicros,
                currencyCode = v.currencyCode,
                precision = v.precisionType,
                network = responseInfo?.mediationAdapterClassName ?: "",
                adUnitId = adUnitId,
                format = format,
                adSource = responseInfo?.loadedAdapterResponseInfo?.adSourceName ?: "",
            ),
        )
    }

    /** [P3-3] Phát sự kiện vòng đời ad qua [AdTracker] (app forward analytics/MMP). */
    private fun track(type: AdEventType, format: String, unitId: String, network: String = "", latencyMs: Long = -1, error: String = "", adSource: String = "") {
        AdTracker.emit(AdEvent(type, format, unitId, network, latencyMs, error, adSource))
    }

    private fun fullScreenCallback(format: String, unitId: String, network: String, callback: FullScreenAdCallback, placement: String = "") = object : FullScreenContentCallback() {
        override fun onAdShowedFullScreenContent() {
            FullScreenAdState.onShown(); AdMetrics.onShown(unitId); AdsLogger.showed(format, unitId, placement)   // [P3-1] SHOWED
            track(AdEventType.SHOWED, format, unitId, network); callback.onAdShowed()
        }
        override fun onAdImpression() { AdsLogger.impression(format, unitId, placement); AdMetrics.onImpression(unitId); track(AdEventType.IMPRESSION, format, unitId, network); callback.onAdImpression() }
        override fun onAdClicked() { AdsLogger.clicked(format, unitId, placement); track(AdEventType.CLICKED, format, unitId, network); callback.onAdClicked() }
        override fun onAdDismissedFullScreenContent() { FullScreenAdState.onClosed(); AdsLogger.dismissed(format, unitId, placement); track(AdEventType.DISMISSED, format, unitId, network); callback.onAdDismissed() }
        override fun onAdFailedToShowFullScreenContent(e: AdError) { FullScreenAdState.onClosed(); AdsLogger.failedToShow(format, unitId, e.message, placement); callback.onAdFailedToShow(e.message) }
    }

    /** Gate cứng: chưa được phép request ad (consent [P2-3] / remote config [P2-5]) → fail ngay, không gọi AdMob. */
    private fun consentGate(format: String, adUnitId: String, result: AdLoadResult, placement: String = ""): Boolean {
        if (AdsSdk.canLoadAds()) return true
        AdsLogger.failed(format, adUnitId, "blocked: ad request not allowed (consent/remote-config)", placement)
        result.onFailed("ads not allowed (consent/remote-config)")
        return false
    }

    // ---- Interstitial ----
    override fun loadInterstitial(context: Context, adUnitId: String, result: AdLoadResult, preload: Boolean, placement: String) {
        if (!consentGate("Interstitial", adUnitId, result, placement)) return
        AdsLogger.startLoad("Interstitial", adUnitId, preload, placement)
        AdMetrics.onRequest(adUnitId)
        track(AdEventType.LOAD_START, "Interstitial", adUnitId)
        val start = System.currentTimeMillis()
        InterstitialAd.load(
            context, adUnitId, request(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    val ms = System.currentTimeMillis() - start
                    AdsLogger.loaded("Interstitial", adUnitId, placement)
                    AdMetrics.onLoaded(adUnitId, ms)
                    track(AdEventType.LOADED, "Interstitial", adUnitId, ad.responseInfo?.mediationAdapterClassName ?: "", ms, adSource = ad.responseInfo?.loadedAdapterResponseInfo?.adSourceName ?: "")
                    ad.onPaidEventListener = OnPaidEventListener { emitPaid("Interstitial", adUnitId, it, ad.responseInfo) }
                    result.onLoaded(AdHandle(ad, adUnitId, placement))
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    AdsLogger.failed("Interstitial", adUnitId, describe(error), placement)
                    AdMetrics.onFailed(adUnitId)
                    track(AdEventType.LOAD_FAILED, "Interstitial", adUnitId, error = error.message)
                    result.onFailed(error.message)
                }
            },
        )
    }

    override fun showInterstitial(activity: Activity, handle: AdHandle, callback: FullScreenAdCallback) {
        val ad = handle.payload as? InterstitialAd ?: run { callback.onAdFailedToShow("invalid handle"); return }
        ad.fullScreenContentCallback = fullScreenCallback("Interstitial", handle.adUnitId, ad.responseInfo?.mediationAdapterClassName ?: "", callback, handle.placement)
        ad.show(activity)
    }

    // ---- App Open ----
    override fun loadAppOpen(context: Context, adUnitId: String, result: AdLoadResult, preload: Boolean, placement: String) {
        if (!consentGate("AppOpen", adUnitId, result, placement)) return
        AdsLogger.startLoad("AppOpen", adUnitId, preload, placement)
        AdMetrics.onRequest(adUnitId)
        track(AdEventType.LOAD_START, "AppOpen", adUnitId)
        val start = System.currentTimeMillis()
        AppOpenAd.load(
            context, adUnitId, request(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    val ms = System.currentTimeMillis() - start
                    AdsLogger.loaded("AppOpen", adUnitId, placement)
                    AdMetrics.onLoaded(adUnitId, ms)
                    track(AdEventType.LOADED, "AppOpen", adUnitId, ad.responseInfo?.mediationAdapterClassName ?: "", ms, adSource = ad.responseInfo?.loadedAdapterResponseInfo?.adSourceName ?: "")
                    ad.onPaidEventListener = OnPaidEventListener { emitPaid("AppOpen", adUnitId, it, ad.responseInfo) }
                    result.onLoaded(AdHandle(ad, adUnitId, placement))
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    AdsLogger.failed("AppOpen", adUnitId, describe(error), placement)
                    AdMetrics.onFailed(adUnitId)
                    track(AdEventType.LOAD_FAILED, "AppOpen", adUnitId, error = error.message)
                    result.onFailed(error.message)
                }
            },
        )
    }

    override fun showAppOpen(activity: Activity, handle: AdHandle, callback: FullScreenAdCallback) {
        val ad = handle.payload as? AppOpenAd ?: run { callback.onAdFailedToShow("invalid handle"); return }
        ad.fullScreenContentCallback = fullScreenCallback("AppOpen", handle.adUnitId, ad.responseInfo?.mediationAdapterClassName ?: "", callback, handle.placement)
        ad.show(activity)
    }

    // ---- Rewarded ----
    override fun loadRewarded(context: Context, adUnitId: String, result: AdLoadResult, preload: Boolean, placement: String) {
        if (!consentGate("Rewarded", adUnitId, result, placement)) return
        AdsLogger.startLoad("Rewarded", adUnitId, preload, placement)
        AdMetrics.onRequest(adUnitId)
        track(AdEventType.LOAD_START, "Rewarded", adUnitId)
        val start = System.currentTimeMillis()
        RewardedAd.load(
            context, adUnitId, request(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    val ms = System.currentTimeMillis() - start
                    AdsLogger.loaded("Rewarded", adUnitId, placement)
                    AdMetrics.onLoaded(adUnitId, ms)
                    track(AdEventType.LOADED, "Rewarded", adUnitId, ad.responseInfo?.mediationAdapterClassName ?: "", ms, adSource = ad.responseInfo?.loadedAdapterResponseInfo?.adSourceName ?: "")
                    ad.onPaidEventListener = OnPaidEventListener { emitPaid("Rewarded", adUnitId, it, ad.responseInfo) }
                    result.onLoaded(AdHandle(ad, adUnitId, placement))
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    AdsLogger.failed("Rewarded", adUnitId, describe(error), placement)
                    AdMetrics.onFailed(adUnitId)
                    track(AdEventType.LOAD_FAILED, "Rewarded", adUnitId, error = error.message)
                    result.onFailed(error.message)
                }
            },
        )
    }

    override fun showRewarded(activity: Activity, handle: AdHandle, callback: RewardAdCallback) {
        val ad = handle.payload as? RewardedAd ?: run { callback.onAdFailedToShow("invalid handle"); return }
        ad.fullScreenContentCallback = fullScreenCallback("Rewarded", handle.adUnitId, ad.responseInfo?.mediationAdapterClassName ?: "", callback, handle.placement)
        ad.show(activity) { reward ->
            AdsLogger.reward("Rewarded", handle.adUnitId, reward.type, reward.amount, handle.placement)
            track(AdEventType.REWARD, "Rewarded", handle.adUnitId, ad.responseInfo?.mediationAdapterClassName ?: "")
            callback.onUserEarnedReward(reward.type, reward.amount)
        }
    }

    // ---- Banner ----
    override fun createBanner(context: Context, adUnitId: String, callback: BannerAdCallback, placement: String): View {
        val adView = AdView(context)
        adView.adUnitId = adUnitId
        val metrics = context.resources.displayMetrics
        val widthDp = (metrics.widthPixels / metrics.density).toInt()
        adView.setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp))
        adView.onPaidEventListener = OnPaidEventListener { emitPaid("Banner", adUnitId, it, adView.responseInfo) }
        val start = System.currentTimeMillis()
        adView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                val ms = System.currentTimeMillis() - start
                AdsLogger.loaded("Banner", adUnitId, placement); AdMetrics.onLoaded(adUnitId, ms)
                track(AdEventType.LOADED, "Banner", adUnitId, adView.responseInfo?.mediationAdapterClassName ?: "", ms, adSource = adView.responseInfo?.loadedAdapterResponseInfo?.adSourceName ?: ""); callback.onLoaded()
            }
            override fun onAdFailedToLoad(error: LoadAdError) { AdsLogger.failed("Banner", adUnitId, describe(error), placement); AdMetrics.onFailed(adUnitId); track(AdEventType.LOAD_FAILED, "Banner", adUnitId, error = error.message); callback.onFailed(error.message) }
            override fun onAdImpression() { AdsLogger.impression("Banner", adUnitId, placement); AdMetrics.onImpression(adUnitId); track(AdEventType.IMPRESSION, "Banner", adUnitId, adView.responseInfo?.mediationAdapterClassName ?: ""); callback.onImpression() }
            override fun onAdClicked() { AdsLogger.clicked("Banner", adUnitId, placement); track(AdEventType.CLICKED, "Banner", adUnitId); callback.onClicked() }
        }
        AdsLogger.startLoad("Banner", adUnitId, placement = placement)
        AdMetrics.onRequest(adUnitId)
        track(AdEventType.LOAD_START, "Banner", adUnitId)
        adView.loadAd(request())
        return adView
    }

    override fun destroyBanner(view: View) {
        (view as? AdView)?.destroy()
    }

    // ---- Native (tách load / bind / destroy — M2a) ----
    override fun loadNative(context: Context, adUnitId: String, result: AdLoadResult, preload: Boolean, placement: String) {
        if (!consentGate("Native", adUnitId, result, placement)) return
        AdsLogger.startLoad("Native", adUnitId, preload, placement)
        AdMetrics.onRequest(adUnitId)
        track(AdEventType.LOAD_START, "Native", adUnitId)
        val start = System.currentTimeMillis()
        val loader = AdLoader.Builder(context, adUnitId)
            .forNativeAd { nativeAd ->
                val ms = System.currentTimeMillis() - start
                AdsLogger.loaded("Native", adUnitId, placement)
                AdMetrics.onLoaded(adUnitId, ms)
                track(AdEventType.LOADED, "Native", adUnitId, nativeAd.responseInfo?.mediationAdapterClassName ?: "", ms, adSource = nativeAd.responseInfo?.loadedAdapterResponseInfo?.adSourceName ?: "")
                nativeAd.setOnPaidEventListener { emitPaid("Native", adUnitId, it, nativeAd.responseInfo) }
                result.onLoaded(AdHandle(nativeAd, adUnitId, placement))   // KHÔNG bind ở đây
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    AdsLogger.failed("Native", adUnitId, describe(error), placement)
                    AdMetrics.onFailed(adUnitId)
                    track(AdEventType.LOAD_FAILED, "Native", adUnitId, error = error.message)
                    result.onFailed(error.message)
                }
                // Native cũng phải đo impression/click như banner — thiếu thì show-rate & attribution native mù.
                override fun onAdImpression() {
                    AdsLogger.impression("Native", adUnitId, placement)
                    AdMetrics.onImpression(adUnitId)
                    track(AdEventType.IMPRESSION, "Native", adUnitId)
                }
                override fun onAdClicked() {
                    AdsLogger.clicked("Native", adUnitId, placement)
                    track(AdEventType.CLICKED, "Native", adUnitId)
                }
            })
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT) // N04: AdChoices trong khung
                    .build(),
            )
            .build()
        loader.loadAd(request())
    }

    override fun bindNative(context: Context, handle: AdHandle, layoutResId: Int): View =
        NativeAdBinder.bind(context, handle.payload as NativeAd, layoutResId)

    override fun destroyNative(view: View) = NativeAdBinder.destroyView(view)

    override fun destroyNativeHandle(handle: AdHandle) {
        (handle.payload as? NativeAd)?.destroy()
    }
}
