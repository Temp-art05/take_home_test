package com.github.dinhn.ads.core

import android.app.Activity
import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Entry point của SDK (facade). Cung cấp primitive cấp thấp (load/show theo id, handle + callback)
 * cho tầng app tự orchestrate theo từng placement.
 *
 * init() tự nạp adapter mặc định (ads-admob) qua reflection; hoặc truyền adapter tường minh.
 * Nếu [AdsConfig.trackWithFirebase] = true, init() cũng tự nạp `FirebaseAdForwarder` (module
 * ads-firebase) qua reflection để tự forward ILRD → Firebase — app chỉ bật cờ, không viết mapping.
 */
object AdsSdk {

    @Volatile
    private var initialized = false

    private var adapter: AdsAdapter? = null

    lateinit var config: AdsConfig
        private set

    lateinit var toggles: AdRemoteToggle
        private set

    private val _ready = mutableStateOf(false)
    private val _canRequestAds = mutableStateOf(true)
    private val _remoteConfigReady = mutableStateOf(true)
    // [P2-5] Coroutine đang chờ RC áp (show wait-timeout). Truy cập trên main thread (scope Main.immediate).
    private val remoteConfigWaiters = mutableListOf<() -> Unit>()

    // Hook forward doanh thu/sự kiện của app + forwarder Firebase nội bộ (nạp qua reflection khi bật cờ).
    @Volatile private var customPaidListener: OnPaidEvent? = null
    @Volatile private var customAdEventListener: OnAdEvent? = null
    // Forwarder Firebase (cùng 1 instance hiện thực cả OnPaidEvent + OnAdEvent) — nạp qua reflection.
    @Volatile private var firebasePaidForwarder: OnPaidEvent? = null
    @Volatile private var firebaseEventForwarder: OnAdEvent? = null
    // [P2-1] Danh sách user-tier để bắn paid_ad_impression_<tier> (rỗng = không bắn tier).
    @Volatile private var userTierLabels: List<String> = emptyList()

    /**
     * true khi adapter đã `initialize()` xong → AN TOÀN để request ad.
     * Observable trong Compose: banner/native [AdsBanner]/[AdsNative] chờ flag này
     * rồi mới load, tránh lỗi "Internal error" do request trước khi MobileAds init xong.
     */
    val ready: State<Boolean> get() = _ready

    /**
     * [P2-3] true ⇒ được phép request ad theo chính sách consent.
     * Nếu [AdsConfig.requireConsentBeforeRequest] = false → luôn true.
     * Nếu = true → false cho tới khi [requestConsent] trả `canRequestAds`.
     */
    val canRequestAds: State<Boolean> get() = _canRequestAds

    /**
     * [P2-5] true ⇒ được phép request ad theo remote config. Nếu
     * [AdsConfig.requireRemoteConfigBeforeRequest] = false → luôn true (không đổi hành vi).
     * Nếu = true → false cho tới khi app gọi [setRemoteConfigReady] (áp remote config lần đầu).
     * Observable trong Compose: banner/native re-fire request khi cờ này lật true.
     */
    val remoteConfigReady: State<Boolean> get() = _remoteConfigReady

    fun init(app: Application, config: AdsConfig, adapter: AdsAdapter? = null) {
        this.config = config
        AdsLogger.enabled = config.isShowLogAds
        this.toggles = AdRemoteToggle(config.remoteToggles)
        _canRequestAds.value = !config.requireConsentBeforeRequest
        _remoteConfigReady.value = !config.requireRemoteConfigBeforeRequest
        // [P2-1/ILRD] Cờ bật ⇒ lib tự nạp forwarder Firebase (module :ads-firebase) qua reflection.
        // 1 instance hiện thực cả OnPaidEvent (revenue) + OnAdEvent (lifecycle). Thiếu module/Firebase
        // ⇒ null → bỏ qua an toàn.
        if (config.trackWithFirebase) {
            val fwd = reflectFirebaseForwarder(app)
            firebasePaidForwarder = fwd as? OnPaidEvent
            firebaseEventForwarder = fwd as? OnAdEvent
        }
        wireTracking()
        // [P2-1] Tự phân user-tier theo quốc gia (bảng GeoTier) → bắn paid_ad_impression_<tier>.
        if (config.autoGeoTier) {
            val country = runCatching { app.resources.configuration.locales[0].country }.getOrNull()
            val tier = GeoTier.tierOf(country, config.geoTierTable)
            setUserTier(listOf(tier))
            AdsLogger.d("autoGeoTier: country='$country' -> tier='$tier'")
        }

        if (!config.enabled) {
            AdsLogger.d("Ads DISABLED -> no-op mode")
            initialized = true
            _ready.value = true
            return
        }

        val resolved = adapter ?: reflectDefaultAdapter()
        if (resolved == null) {
            AdsLogger.e("Không tìm thấy AdsAdapter. Thêm module ads-admob hoặc truyền adapter vào init().")
            initialized = true
            _ready.value = true
            return
        }
        this.adapter = resolved

        resolved.initialize(app, config.testMode, config.testDeviceIds) {
            AdsLogger.d("Adapter ${resolved.network} initialized")
            _ready.value = true
        }
        initialized = true
    }

    /**
     * Adapter thô (load/show theo id + handle) — **NỘI BỘ lib**. Các composable/manager trong core
     * (FullScreenAdManager/NativeAdPool/AdsBanner…) dùng; app KHÔNG gọi, luôn đi qua placement API
     * (register* → preload/show theo tên / AdsBanner / AdsNative). null nếu SDK tắt / chưa init.
     */
    internal fun adapter(): AdsAdapter? = adapter

    fun isInitialized(): Boolean = initialized

    /* ---- Full-screen placement (inter / reward / app-open) ----
     * App đăng ký placement 1 lần (id + enable key — id vẫn thuộc app, Rule 3) rồi gọi preload/show
     * theo TÊN. Lib (`FullScreenAdManager`) tự: waterfall theo toggle → load nếu chưa có → loading
     * (inter/reward) → show → frequency cap → preload lại. Xem [FullScreenAdManager]. */

    /** Đăng ký 1 placement interstitial. [minLoadingMs] giữ loading tối thiểu trước khi show. */
    fun registerInterstitial(
        name: String,
        highFloorId: String,
        normalId: String,
        enableHighFloorKey: String,
        enableNormalKey: String,
        minLoadingMs: Long = 500L,
        loadTimeoutMillis: Long = 30_000L,
        minIntervalMs: Long = FrequencyCapper.defaultMinIntervalMs,
        maxPerSession: Int = FrequencyCapper.defaultMaxPerSession,
        // FlowDrama: cho app tắt AdsLoadingOverlay (bản gốc gắn cứng true) — app chỉ show ad đã preload sẵn.
        showLoading: Boolean = true,
    ) = FullScreenAdManager.register(
        FullScreenPlacement(name, FullScreenFormat.INTERSTITIAL, highFloorId, normalId,
            enableHighFloorKey, enableNormalKey, showLoading, minLoadingMs, loadTimeoutMillis, minIntervalMs, maxPerSession),
    )

    /** Đăng ký 1 placement rewarded (có loading trước khi show). */
    fun registerRewarded(
        name: String,
        highFloorId: String,
        normalId: String,
        enableHighFloorKey: String,
        enableNormalKey: String,
        minLoadingMs: Long = 500L,
        loadTimeoutMillis: Long = 30_000L,
        minIntervalMs: Long = 0L,
        maxPerSession: Int = Int.MAX_VALUE,
        // FlowDrama: cho app tắt AdsLoadingOverlay (bản gốc gắn cứng true) — app chỉ show ad đã preload sẵn.
        showLoading: Boolean = true,
    ) = FullScreenAdManager.register(
        FullScreenPlacement(name, FullScreenFormat.REWARDED, highFloorId, normalId,
            enableHighFloorKey, enableNormalKey, showLoading, minLoadingMs, loadTimeoutMillis, minIntervalMs, maxPerSession),
    )

    /**
     * Đăng ký 1 placement app-open (KHÔNG loading — xem quyết định app-open).
     * [screenGuardColor] = màu nền opaque phủ phía sau ad lúc show để KHÔNG lộ nội dung app khi resume
     * (mặc định trắng). Truyền `null` để tắt, hoặc màu khác (vd nền tối) cho khớp app.
     */
    fun registerAppOpen(
        name: String,
        highFloorId: String,
        normalId: String,
        enableHighFloorKey: String,
        enableNormalKey: String,
        loadTimeoutMillis: Long = 30_000L,
        screenGuardColor: Int? = android.graphics.Color.WHITE,
    ) = FullScreenAdManager.register(
        FullScreenPlacement(name, FullScreenFormat.APP_OPEN, highFloorId, normalId,
            enableHighFloorKey, enableNormalKey, showLoading = false, minLoadingMs = 0L, loadTimeoutMillis, minIntervalMs = 0L, maxPerSession = Int.MAX_VALUE,
            screenGuardColor = screenGuardColor),
    )

    /**
     * Preload ngầm placement full-screen vào cache. [context] có thể là Application (app-open).
     * [onLoaded]/[onLoadFailed] báo kết quả load (mặc định no-op).
     */
    fun preloadFullScreen(
        context: android.content.Context,
        name: String,
        onLoaded: () -> Unit = {},
        onLoadFailed: () -> Unit = {},
    ) = FullScreenAdManager.preload(context, name, onLoaded, onLoadFailed)

    fun isFullScreenReady(name: String): Boolean = FullScreenAdManager.isReady(name)
    fun isFullScreenEnabled(name: String): Boolean = FullScreenAdManager.isEnabled(name)

    /**
     * Show interstitial / app-open theo tên. Loading-trước-show (inter), waterfall, cap, preload lại
     * đều do lib lo. Không block flow: [onNext] khi đóng / lỗi / không có ad. [activity] = [LifecycleOwner].
     */
    fun showInterstitial(
        activity: Activity,
        name: String,
        onAdShowed: () -> Unit = {},
        onNext: () -> Unit = {},
        onLoading: (Boolean) -> Unit = {},
    ) = FullScreenAdManager.show(activity, name, onAdShowed, onNext, onLoading)

    /** Show interstitial với callback **đầy đủ vòng đời** ([FullScreenAdCallbacks]). */
    fun showInterstitial(activity: Activity, name: String, callbacks: FullScreenAdCallbacks) =
        FullScreenAdManager.show(activity, name, callbacks)

    /** Show app-open theo tên (không loading). Dùng cho show thủ công; auto-show resume do app quản. */
    fun showAppOpen(
        activity: Activity,
        name: String,
        onAdShowed: () -> Unit = {},
        onNext: () -> Unit = {},
    ) = FullScreenAdManager.show(activity, name, onAdShowed, onNext)

    /** Show app-open với callback đầy đủ vòng đời ([FullScreenAdCallbacks]). */
    fun showAppOpen(activity: Activity, name: String, callbacks: FullScreenAdCallbacks) =
        FullScreenAdManager.show(activity, name, callbacks)

    /** Show rewarded theo tên. [onNext] báo `userEarned`; [onUserEarnedReward] khi user nhận thưởng. */
    fun showRewarded(
        activity: Activity,
        name: String,
        onUserEarnedReward: (type: String, amount: Int) -> Unit = { _, _ -> },
        onAdShowed: () -> Unit = {},
        onNext: (userEarned: Boolean) -> Unit = {},
        onLoading: (Boolean) -> Unit = {},
    ) = FullScreenAdManager.showRewarded(activity, name, onUserEarnedReward, onAdShowed, onNext, onLoading)

    /** Show rewarded với callback đầy đủ vòng đời ([FullScreenAdCallbacks], override `onUserEarnedReward`). */
    fun showRewarded(activity: Activity, name: String, callbacks: FullScreenAdCallbacks) =
        FullScreenAdManager.showRewarded(activity, name, callbacks)

    /* ---- View-based placement (banner / native) ----
     * App đăng ký id + key theo tên rồi render bằng composable `AdsBanner(name)` / `AdsNative(name, layout)`.
     * Layout XML native vẫn ở app (Rule 1) → truyền vào composable. Xem [ViewAdRegistry]. */

    /** Đăng ký 1 placement banner (waterfall HF→AP). Render bằng composable `AdsBanner(name)`. */
    fun registerBanner(name: String, highFloorId: String, normalId: String, enableHighFloorKey: String, enableNormalKey: String) =
        ViewAdRegistry.register(ViewAdPlacement(name, highFloorId, normalId, enableHighFloorKey, enableNormalKey))

    /** Đăng ký 1 placement native (waterfall HF→AP). Render bằng composable `AdsNative(name, layoutId)`. */
    fun registerNative(name: String, highFloorId: String, normalId: String, enableHighFloorKey: String, enableNormalKey: String) =
        ViewAdRegistry.register(ViewAdPlacement(name, highFloorId, normalId, enableHighFloorKey, enableNormalKey))

    /** Placement banner/native còn bật (ít nhất 1 trong 2 key)? */
    fun isViewEnabled(name: String): Boolean = ViewAdRegistry.isEnabled(name)

    /** Warm-up native vào [NativeAdPool] (waterfall HF→AP theo toggle). Composable `AdsNative` cũng tự request. */
    fun preloadNative(context: android.content.Context, name: String) =
        NativeAdPool.request(context.applicationContext, name, ViewAdRegistry.waterfallIds(name))

    /* ---- Log ad ---- */
    val isShowLogAds: Boolean get() = AdsLogger.enabled

    /** [P3-2] true khi được phép bật/tắt log runtime (init + `AdsConfig.logToggleAllowed`). */
    fun canToggleLog(): Boolean = initialized && config.logToggleAllowed

    /** Bật/tắt log ad runtime. No-op nếu KHÔNG được phép ([canToggleLog]). */
    fun setShowLogAds(enabled: Boolean) {
        if (!canToggleLog()) return
        AdsLogger.enabled = enabled
        AdsLogger.d("isShowLogAds = $enabled")
    }

    /** Đảo trạng thái log (nếu được phép) và trả về trạng thái mới — để app hiện Toast. */
    fun toggleShowLogAds(): Boolean {
        if (!canToggleLog()) return AdsLogger.enabled
        val newState = !AdsLogger.enabled
        AdsLogger.enabled = newState
        AdsLogger.d("isShowLogAds = $newState")
        return newState
    }

    /** true khi đang có 1 full-screen ad (inter/reward/appopen) hiển thị — app-open check trước khi tự show. */
    val isFullScreenAdShowing: Boolean get() = FullScreenAdState.isShowing

    /** Mili-giây kể từ lần full-screen (inter/reward/appopen) show gần nhất; MAX nếu chưa show lần nào.
     *  App-open resume dùng làm cooldown để không show đè liền sau interstitial/reward (P2-2). */
    fun millisSinceLastFullScreenShown(): Long =
        if (FullScreenAdState.lastShownAt == 0L) Long.MAX_VALUE
        else System.currentTimeMillis() - FullScreenAdState.lastShownAt

    /** Chỉnh min-interval mặc định giữa 2 lần show interstitial (ms) — app tune từ remote config. */
    fun setDefaultInterstitialMinIntervalMs(ms: Long) {
        FrequencyCapper.defaultMinIntervalMs = ms
    }

    /* ---- Remote toggle (mỗi placement 2 key: high floor + all price) ---- */
    fun isAdEnabled(key: String): Boolean =
        initialized && config.enabled && toggles.isEnabled(key)

    fun setRemoteToggle(key: String, enabled: Boolean) {
        if (initialized) toggles.set(key, enabled)
    }

    fun setRemoteToggles(values: Map<String, Boolean>) {
        if (initialized) toggles.setAll(values)
    }

    /* ---- Revenue (ILRD) ---- */
    /** Cài 1 listener nội bộ ở AdRevenue/AdTracker để fan-out cho hook app + forwarder Firebase. */
    private fun wireTracking() {
        AdRevenue.setListener(OnPaidEvent { value ->
            customPaidListener?.onPaid(value)
            firebasePaidForwarder?.let { runCatching { it.onPaid(value) } }
        })
        AdTracker.setListener(OnAdEvent { event ->
            customAdEventListener?.onEvent(event)
            firebaseEventForwarder?.let { runCatching { it.onEvent(event) } }
        })
    }

    /** Hook custom (tuỳ chọn) — chạy song song với forwarder Firebase (nếu bật `trackWithFirebase`). */
    fun setOnPaidEvent(listener: OnPaidEvent?) { customPaidListener = listener }

    /* ---- Tracking sự kiện ad ([P3-3]) — app tự forward sang analytics/MMP ---- */
    fun setOnAdEvent(listener: OnAdEvent?) { customAdEventListener = listener }

    /* ---- User tier (P2-1) — bắn paid_ad_impression_<tier> khi trackWithFirebase ---- */
    /**
     * Khai báo (các) label user-tier để forwarder Firebase bắn thêm `paid_ad_impression_<label>`
     * cho mỗi impression (bundle giống `paid_ad_impression`). Label `"user_default"` bị bỏ qua.
     * Rỗng (mặc định) ⇒ không bắn event tier. Gọi bất kỳ lúc nào (vd sau khi phân tier user).
     */
    fun setUserTier(labels: List<String>) { userTierLabels = labels }

    /** Danh sách user-tier hiện tại (forwarder Firebase đọc lúc impression). */
    fun userTier(): List<String> = userTierLabels

    /* ---- Consent + remote-config gate ---- */
    /**
     * true khi được phép request ad. **Gate cứng cho MỌI load** (adapter load* + composable
     * banner/native). Gồm: init xong + [AdsConfig.enabled] + consent ([P2-3]) + remote-config
     * đã áp ([P2-5], nếu bật [AdsConfig.requireRemoteConfigBeforeRequest]).
     */
    fun canLoadAds(): Boolean =
        initialized && config.enabled && _canRequestAds.value && _remoteConfigReady.value

    /** Cập nhật trạng thái consent. Không yêu cầu consent ⇒ luôn cho phép. */
    fun setConsentResult(canRequestAds: Boolean) {
        _canRequestAds.value = if (config.requireConsentBeforeRequest) canRequestAds else true
    }

    /**
     * [P2-5] App gọi sau khi áp remote config LẦN ĐẦU (fetch RC xong / dùng cache) → mở khoá request
     * cho MỌI loại ad + đánh thức show đang chờ ([awaitRemoteConfigReady]) + flush preload đã hoãn
     * ([FullScreenAdManager.onRemoteConfigReady]). Idempotent (gọi lại nhiều lần vô hại). No-op thực
     * tế khi không bật gate ([AdsConfig.requireRemoteConfigBeforeRequest] = false) vì cờ đã true sẵn.
     */
    fun setRemoteConfigReady() {
        _remoteConfigReady.value = true
        if (remoteConfigWaiters.isNotEmpty()) {
            val waiters = remoteConfigWaiters.toList()
            remoteConfigWaiters.clear()
            waiters.forEach { it() }
        }
        FullScreenAdManager.onRemoteConfigReady()
    }

    /**
     * [P2-5] Suspend tới khi remote config được áp ([setRemoteConfigReady]); trả ngay nếu đã ready.
     * Nội bộ core — dùng cho "wait-timeout" khi show full-screen lúc RC chưa về; caller bọc bằng
     * `withTimeoutOrNull(loadTimeoutMillis)` nên KHÔNG chờ vô hạn.
     */
    internal suspend fun awaitRemoteConfigReady() {
        if (_remoteConfigReady.value) return
        suspendCancellableCoroutine<Unit> { cont ->
            val waiter = { if (cont.isActive) cont.resumeWith(Result.success(Unit)) }
            remoteConfigWaiters.add(waiter)
            cont.invokeOnCancellation { remoteConfigWaiters.remove(waiter) }
        }
    }

    fun requestConsent(activity: Activity, onResult: (canRequestAds: Boolean) -> Unit = {}) {
        if (!isInitialized() || !config.enabled) { onResult(false); return }
        ConsentManager.request(activity) { canReq ->
            setConsentResult(canReq)
            onResult(canReq)
        }
    }

    private fun reflectDefaultAdapter(): AdsAdapter? = try {
        val clazz = Class.forName("com.github.dinhn.ads.admob.AdMobAdapter")
        clazz.getDeclaredConstructor().newInstance() as AdsAdapter
    } catch (t: Throwable) {
        AdsLogger.w("reflectDefaultAdapter: ${t.message}")
        null
    }

    /**
     * Nạp instance `FirebaseAdForwarder(Context)` (hiện thực cả [OnPaidEvent] + [OnAdEvent]) từ module
     * `:ads-firebase` qua reflection (core KHÔNG phụ thuộc Firebase — Rule 1). Caller tự cast sang từng
     * interface. Thiếu module hoặc Firebase chưa init ⇒ null (bỏ qua, không crash).
     */
    private fun reflectFirebaseForwarder(app: Application): Any? = try {
        val clazz = Class.forName("com.github.dinhn.ads.firebase.FirebaseAdForwarder")
        clazz.getDeclaredConstructor(android.content.Context::class.java).newInstance(app)
    } catch (t: Throwable) {
        AdsLogger.w("reflectFirebaseForwarder (đã bật trackWithFirebase nhưng chưa add :ads-firebase?): ${t.message}")
        null
    }
}
