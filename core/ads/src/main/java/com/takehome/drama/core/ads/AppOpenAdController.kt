package com.takehome.drama.core.ads

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.takehome.drama.core.ads.sdk.AdsSdkFacade
import com.takehome.drama.domain.model.ads.AdPlacement
import com.takehome.drama.domain.usecase.ads.IsAdPlacementEnabledUseCase
import java.lang.ref.WeakReference
import timber.log.Timber

/**
 * App-open khi user quay lại app từ nền (như `AppOpenAdManager` của HomeMorph). Hiện khi đủ cả:
 * không phải lần mở đầu · Remote Config đã áp · placement bật · không bị bỏ qua có chủ đích
 * ([suppressNextResume]) · không có màn video nào đang chặn ([block]) · không có full-screen khác
 * đang hiện · cách full-screen trước ≥ [COOLDOWN_AFTER_FULLSCREEN_MS].
 *
 * Chỉ chạy trên main thread (lifecycle callback + composition).
 */
class AppOpenAdController(
    private val app: Application,
    private val sdk: AdsSdkFacade,
    private val isEnabled: IsAdPlacementEnabledUseCase,
) {
    private val name = AdPlacement.AppOpenResume.id
    private var started = false
    private var coldStart = true
    private var remoteConfigReady = false
    private var skipNextResume = false
    private var blockers = 0
    private var visibleActivity: WeakReference<Activity>? = null

    fun start() {
        if (started) return
        started = true
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) = onActivityVisible(activity)
            override fun onActivityResumed(activity: Activity) = onActivityVisible(activity)
            override fun onActivityStopped(activity: Activity) = onActivityHidden(activity)
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) = onAppForeground()
        })
    }

    fun onRemoteConfigReady() {
        remoteConfigReady = true
        preload()
    }

    /** Gọi ngay trước khi chủ động rời app (xin quyền, mở app khác) — lần quay lại kế tiếp không hiện ad. */
    fun suppressNextResume() {
        skipNextResume = true
    }

    /** Màn đang có video gọi khi hiện; trả về hàm nhả (gọi nhiều lần vẫn chỉ nhả một). */
    fun block(): () -> Unit {
        blockers++
        var released = false
        return {
            if (!released) {
                released = true
                blockers--
            }
        }
    }

    internal fun onActivityVisible(activity: Activity) {
        visibleActivity = WeakReference(activity)
    }

    internal fun onActivityHidden(activity: Activity) {
        if (visibleActivity?.get() === activity) visibleActivity = null
    }

    internal fun onAppForeground() {
        if (coldStart) {
            coldStart = false
            return
        }
        if (!remoteConfigReady) return
        if (skipNextResume) {
            skipNextResume = false
            preload()
            return
        }
        if (blockers > 0 || !isEnabled(AdPlacement.AppOpenResume)) return
        val activity = visibleActivity?.get() ?: return
        val canShow = runCatching {
            !sdk.isFullScreenAdShowing() && sdk.millisSinceLastFullScreenShown() >= COOLDOWN_AFTER_FULLSCREEN_MS
        }.getOrDefault(false)
        if (!canShow) return
        runCatching { sdk.showAppOpen(activity, name) }.onFailure { Timber.tag(TAG).w(it, "app open show failed") }
    }

    private fun preload() {
        if (!isEnabled(AdPlacement.AppOpenResume)) return
        runCatching { sdk.preloadFullScreen(app, name, {}, {}) }.onFailure { Timber.tag(TAG).w(it, "app open preload failed") }
    }

    companion object {
        const val COOLDOWN_AFTER_FULLSCREEN_MS = 15_000L
        private const val TAG = "Ads"
    }
}
