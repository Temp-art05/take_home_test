package com.github.dinhn.ads.core

import android.app.Activity
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

/** Loại full-screen ad mà [FullScreenAdManager] quản. */
enum class FullScreenFormat { INTERSTITIAL, REWARDED, APP_OPEN }

/**
 * Cấu hình MỘT placement full-screen — app đăng ký 1 lần qua `AdsSdk.register*`.
 * Ad unit id vẫn do app cấp (Rule 3); SDK chỉ giữ để tự waterfall/load.
 *
 * [showLoading] = hiện [AdsLoadingOverlay] trước khi show (inter/reward = true, app-open = false —
 * xem quyết định app-open không loading). Cap qua [minIntervalMs]/[maxPerSession] (0/MAX = không cap).
 *
 * [screenGuardColor] = màu nền [AdScreenGuard] phủ phía sau ad lúc show để KHÔNG lộ nội dung app
 * (chủ yếu app-open resume). null = tắt. Mặc định app-open = trắng (xem `AdsSdk.registerAppOpen`).
 */
class FullScreenPlacement internal constructor(
    val name: String,
    val format: FullScreenFormat,
    val highFloorId: String,
    val normalId: String,
    val enableHighFloorKey: String,
    val enableNormalKey: String,
    val showLoading: Boolean,
    val minLoadingMs: Long,
    val loadTimeoutMillis: Long,
    val minIntervalMs: Long,
    val maxPerSession: Int,
    val screenGuardColor: Int? = null,
)

/**
 * Tầng **quản placement full-screen TRONG lib** (inter / reward / app-open). Giữ cache handle theo
 * placement + load waterfall (HF→AP theo toggle) + frequency cap + preload lại sau khi đóng.
 * App chỉ register 1 lần rồi gọi [preload]/[show]/[showRewarded] theo tên — không tự ghép load/cache.
 *
 * **Rule 2 (đã nới):** đây là tầng manager nên ĐƯỢC cache handle. Adapter vẫn stateless —
 * mỗi `load*` trả [AdHandle] mới; manager chỉ giữ tham chiếu để show sau, không cache trong adapter.
 * Loading-trước-show ủy thác [FullScreenAdPresenter]; show qua `AdsSdk.adapter()`.
 *
 * Dùng scope nội bộ (Main + SupervisorJob) thay lifecycleScope → preload được cả khi chỉ có
 * `Application` (app-open preload lúc app start, chưa có activity).
 */
object FullScreenAdManager {

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    private val placements = HashMap<String, FullScreenPlacement>()
    private val handles = HashMap<String, AdHandle?>()
    private val jobs = HashMap<String, Job>()

    /** [P2-5] Preload bị hoãn do RC chưa áp — flush khi `AdsSdk.setRemoteConfigReady()`. Giữ appContext (không leak). */
    private class PendingPreload(val context: Context, val onLoaded: () -> Unit, val onLoadFailed: () -> Unit)
    private val pendingPreloads = HashMap<String, PendingPreload>()

    fun register(placement: FullScreenPlacement) { placements[placement.name] = placement }

    private fun enableHighFloor(p: FullScreenPlacement) = AdsSdk.isAdEnabled(p.enableHighFloorKey)
    private fun enableNormal(p: FullScreenPlacement) = AdsSdk.isAdEnabled(p.enableNormalKey)
    private fun isEnabled(p: FullScreenPlacement) = enableHighFloor(p) || enableNormal(p)
    private fun waterfall(p: FullScreenPlacement): List<String> = buildList {
        if (enableHighFloor(p)) add(p.highFloorId)
        if (enableNormal(p)) add(p.normalId)
    }.filter { it.isNotBlank() }

    fun isEnabled(name: String): Boolean = placements[name]?.let { isEnabled(it) } ?: false
    fun isReady(name: String): Boolean = handles[name] != null

    /** Load 1 id theo format → suspend, trả null khi fail. [placement] = tên để kèm vào log. */
    private suspend fun loadOne(context: Context, format: FullScreenFormat, id: String, preload: Boolean, placement: String): AdHandle? {
        val adapter = AdsSdk.adapter() ?: return null
        return suspendCancellableCoroutine { cont ->
            val cb = object : AdLoadResult {
                override fun onLoaded(handle: AdHandle) { if (cont.isActive) cont.resumeWith(Result.success(handle)) }
                override fun onFailed(error: String) { if (cont.isActive) cont.resumeWith(Result.success(null)) }
            }
            when (format) {
                FullScreenFormat.INTERSTITIAL -> adapter.loadInterstitial(context, id, cb, preload, placement)
                FullScreenFormat.REWARDED -> adapter.loadRewarded(context, id, cb, preload, placement)
                FullScreenFormat.APP_OPEN -> adapter.loadAppOpen(context, id, cb, preload, placement)
            }
        }
    }

    /** Waterfall HF→AP: thử lần lượt id đang bật, có handle là dừng. */
    private suspend fun loadWaterfall(context: Context, p: FullScreenPlacement, preload: Boolean): AdHandle? {
        for (id in waterfall(p)) {
            val handle = loadOne(context, p.format, id, preload, p.name)
            if (handle != null) return handle
        }
        return null
    }

    /**
     * Preload ngầm vào cache (tag [PRE-LOAD]). No-op nếu đã có / đang load / placement tắt / chưa đăng ký.
     * [onLoaded]/[onLoadFailed] báo kết quả load (mặc định no-op).
     */
    fun preload(context: Context, name: String, onLoaded: () -> Unit = {}, onLoadFailed: () -> Unit = {}) {
        val p = placements[name] ?: return
        if (handles[name] != null) { onLoaded(); return }
        // [P2-5] RC chưa áp → HOÃN (không spin coroutine fail), tự chạy lại khi onRemoteConfigReady() flush.
        if (!AdsSdk.remoteConfigReady.value) {
            pendingPreloads[name] = PendingPreload(context.applicationContext, onLoaded, onLoadFailed)
            return
        }
        if (jobs[name]?.isActive == true || !isEnabled(p)) return
        jobs[name] = scope.launch {
            val handle = loadWaterfall(context.applicationContext, p, preload = true)
            handles[name] = handle
            if (handle != null) onLoaded() else onLoadFailed()
        }
    }

    /**
     * [P2-5] RC vừa được áp ([AdsSdk.setRemoteConfigReady]) → chạy lại các preload đã hoãn. Placement
     * remote đã tắt sẽ tự bị `isEnabled` chặn trong [preload]. Idempotent.
     */
    internal fun onRemoteConfigReady() {
        if (pendingPreloads.isEmpty()) return
        val pending = HashMap(pendingPreloads)
        pendingPreloads.clear()
        pending.forEach { (name, pp) -> preload(pp.context, name, pp.onLoaded, pp.onLoadFailed) }
    }

    /** Show inter/app-open theo tên — callback đầy đủ qua [callbacks] ([FullScreenAdCallbacks]). */
    fun show(activity: Activity, name: String, callbacks: FullScreenAdCallbacks) = present(activity, name, callbacks)

    /**
     * Show inter/app-open theo tên (overload gọn). [onAdShowed] = show thành công;
     * [onNext] = đóng / lỗi / không có ad (đi tiếp); [onLoading] = overlay bật/tắt.
     */
    fun show(
        activity: Activity,
        name: String,
        onAdShowed: () -> Unit = {},
        onNext: () -> Unit = {},
        onLoading: (Boolean) -> Unit = {},
    ) = present(activity, name, object : FullScreenAdCallbacks() {
        override fun onAdShowed() = onAdShowed()
        override fun onAdDismissed() = onNext()
        override fun onAdFailedToShow() = onNext()
        override fun onNoAd() = onNext()
        override fun onLoading(showing: Boolean) = onLoading(showing)
    })

    /** Show rewarded theo tên — callback đầy đủ qua [callbacks] (override [FullScreenAdCallbacks.onUserEarnedReward]). */
    fun showRewarded(activity: Activity, name: String, callbacks: FullScreenAdCallbacks) = present(activity, name, callbacks)

    /**
     * Show rewarded theo tên (overload gọn). [onUserEarnedReward] khi nhận thưởng;
     * [onNext] báo `userEarned` (true nếu user nhận thưởng trước khi đóng).
     */
    fun showRewarded(
        activity: Activity,
        name: String,
        onUserEarnedReward: (type: String, amount: Int) -> Unit = { _, _ -> },
        onAdShowed: () -> Unit = {},
        onNext: (userEarned: Boolean) -> Unit = {},
        onLoading: (Boolean) -> Unit = {},
    ) {
        var earned = false
        present(activity, name, object : FullScreenAdCallbacks() {
            override fun onUserEarnedReward(type: String, amount: Int) { earned = true; onUserEarnedReward(type, amount) }
            override fun onAdShowed() = onAdShowed()
            override fun onAdDismissed() = onNext(earned)
            override fun onAdFailedToShow() = onNext(false)
            override fun onNoAd() = onNext(false)
            override fun onLoading(showing: Boolean) = onLoading(showing)
        })
    }

    private fun present(activity: Activity, name: String, cb: FullScreenAdCallbacks) {
        val p = placements[name] ?: run { cb.onNoAd(); return }
        if (FullScreenAdState.isShowing) { cb.onNoAd(); return }                                  // IA04/R04
        if (!FrequencyCapper.canShow(name, p.minIntervalMs, p.maxPerSession)) { cb.onNoAd(); return }  // P2-2
        if (!isEnabled(p)) { cb.onNoAd(); return }

        scope.launch {
            FullScreenAdPresenter.presentWithLoading(
                activity = activity,
                minLoadingMs = p.minLoadingMs,
                loadTimeoutMillis = p.loadTimeoutMillis,
                showOverlay = p.showLoading,
                ready = handles[name],
                load = {
                    // [P2-5] RC chưa về → GIỮ loading chờ RC rồi mới load (không show ad chưa biết trạng
                    // thái toggle). Cả await + load bị bọc bởi loadTimeoutMillis trong presentWithLoading:
                    // RC không về kịp → hết giờ → onNoAd. RC về mà toggle tắt → loadWaterfall rỗng → onNoAd.
                    if (!AdsSdk.remoteConfigReady.value) AdsSdk.awaitRemoteConfigReady()
                    if (jobs[name]?.isActive != true) {
                        jobs[name] = scope.launch { handles[name] = loadWaterfall(activity, p, preload = false) }
                    }
                    jobs[name]?.join()
                    handles[name]
                },
                show = { handle, onShowed, onClosed, onFailed ->
                    handles[name] = null            // tiêu thụ handle vừa lấy
                    jobs.remove(name)
                    FrequencyCapper.recordShown(name)
                    val adapter = AdsSdk.adapter()
                    when {
                        adapter == null -> onClosed()
                        p.format == FullScreenFormat.REWARDED -> adapter.showRewarded(activity, handle, object : RewardAdCallback {
                            override fun onUserEarnedReward(type: String, amount: Int) = cb.onUserEarnedReward(type, amount)
                            override fun onAdShowed() = onShowed()
                            override fun onAdDismissed() = onClosed()
                            override fun onAdFailedToShow(error: String) = onFailed()
                        })
                        p.format == FullScreenFormat.APP_OPEN -> {
                            // Phủ nền opaque phía sau app-open để không lộ nội dung app lúc chuyển cảnh
                            // resume (rõ nhất ở release). Gỡ khi ad đóng / show fail.
                            p.screenGuardColor?.let { AdScreenGuard.show(activity, it) }
                            adapter.showAppOpen(activity, handle, fsCallback(
                                onShowed,
                                { AdScreenGuard.hide(activity); onClosed() },
                                { AdScreenGuard.hide(activity); onFailed() },
                            ))
                        }
                        else ->
                            adapter.showInterstitial(activity, handle, fsCallback(onShowed, onClosed, onFailed))
                    }
                },
                onAdShowed = cb::onAdShowed,
                onAdClosed = { cb.onAdDismissed(); preload(activity, name) },   // đóng → báo + preload lại
                onAdFailed = { cb.onAdFailedToShow(); preload(activity, name) },
                onNoAd = cb::onNoAd,
                onLoading = cb::onLoading,
                onStartLoad = cb::onStartLoad,
                onLoaded = cb::onLoaded,
                onLoadFailed = cb::onLoadFailed,
            )
        }
    }

    private fun fsCallback(onShowed: () -> Unit, onClosed: () -> Unit, onFailed: () -> Unit) =
        object : FullScreenAdCallback {
            override fun onAdShowed() = onShowed()
            override fun onAdDismissed() = onClosed()
            override fun onAdFailedToShow(error: String) = onFailed()
        }
}
