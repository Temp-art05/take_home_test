package com.takehome.drama.core.ads

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import com.takehome.drama.core.ads.sdk.AdsSdkFacade
import com.takehome.drama.domain.config.AppConfig
import com.takehome.drama.domain.fake.FakeAdsConfigRepository
import com.takehome.drama.domain.model.ads.AdPlacement
import com.takehome.drama.domain.model.ads.AdsConfig
import com.takehome.drama.domain.model.ads.PlacementConfig
import com.takehome.drama.domain.usecase.ads.IsAdPlacementEnabledUseCase
import com.takehome.drama.domain.usecase.ads.ObserveAdPlacementUseCase
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdsPresenterTest {

    private class FakeSdk : AdsSdkFacade {
        val ready = mutableSetOf<String>()
        var throwOnShow = false
        var throwOnPreload = false
        val shown = mutableListOf<String>()
        val preloads = mutableListOf<String>()
        /** Callback lần preload gần nhất — test tự gọi để giả lập load xong/lỗi. */
        var pendingLoaded: (() -> Unit)? = null
        var pendingFailed: (() -> Unit)? = null
        /** Kết quả giả của rewarded khi đóng: earned hay không. */
        var rewardEarned = true
        var dismissInterstitial: (() -> Unit)? = null

        override fun isFullScreenReady(name: String) = name in ready

        override fun preloadFullScreen(context: Context, name: String, onLoaded: () -> Unit, onFailed: () -> Unit) {
            if (throwOnPreload) error("sdk not initialised")
            preloads += name
            pendingLoaded = { ready += name; onLoaded() }
            pendingFailed = onFailed
        }

        override fun showInterstitial(activity: Activity, name: String, onDone: () -> Unit) {
            if (throwOnShow) error("boom")
            shown += name
            dismissInterstitial = onDone
        }

        override fun showRewarded(activity: Activity, name: String, onResult: (Boolean) -> Unit) {
            if (throwOnShow) error("boom")
            shown += name
            onResult(rewardEarned)
        }

        override fun requestNative(context: Context, name: String) = Unit
    }

    private val appConfig = AppConfig("", isDebug = true, isDevFlavor = true, useFixtures = true, versionCode = 1, versionName = "1.0.0")
    private val repo = FakeAdsConfigRepository()
    private val sdk = FakeSdk()
    private val presenter = AdsPresenterImpl(
        context = ContextWrapper(null),
        sdk = sdk,
        isEnabled = IsAdPlacementEnabledUseCase(repo, appConfig),
        observeEnabled = ObserveAdPlacementUseCase(repo, appConfig),
    )
    private val activity = Activity()
    private val interstitial = AdPlacement.InterstitialOnboarding.id

    @Test
    fun `placement off - onDone runs once and the sdk is never asked to show`() {
        repo.config.value = AdsConfig(placements = mapOf(interstitial to PlacementConfig(false)))
        sdk.ready += interstitial
        var done = 0
        presenter.showInterstitialIfReady(activity, AdPlacement.InterstitialOnboarding) { done++ }
        assertEquals(1, done)
        assertEquals(emptyList<String>(), sdk.shown)
    }

    @Test
    fun `version blocked - nothing is preloaded`() {
        repo.config.value = AdsConfig(disabledVersions = listOf("1.0.0"))
        presenter.preload(AdPlacement.InterstitialOnboarding)
        assertEquals(emptyList<String>(), sdk.preloads)
    }

    @Test
    fun `not ready yet - go on immediately instead of waiting`() {
        var done = 0
        presenter.showInterstitialIfReady(activity, AdPlacement.InterstitialOnboarding) { done++ }
        assertEquals(1, done)
        assertEquals(emptyList<String>(), sdk.shown)
    }

    @Test
    fun `ready - shows and continues once on dismiss even if the sdk calls back twice`() {
        sdk.ready += interstitial
        var done = 0
        presenter.showInterstitialIfReady(activity, AdPlacement.InterstitialOnboarding) { done++ }
        assertEquals(0, done)
        sdk.dismissInterstitial!!()
        sdk.dismissInterstitial!!()
        assertEquals(1, done)
    }

    @Test
    fun `sdk throws on show - onDone still runs exactly once`() {
        sdk.ready += interstitial
        sdk.throwOnShow = true
        var done = 0
        presenter.showInterstitialIfReady(activity, AdPlacement.InterstitialOnboarding) { done++ }
        assertEquals(1, done)
    }

    @Test
    fun `sdk throws on preload - swallowed`() {
        sdk.throwOnPreload = true
        presenter.preload(AdPlacement.InterstitialOnboarding)
    }

    @Test
    fun `rewarded loads - true`() = runTest {
        var result: Boolean? = null
        val job = launchLoad { result = it }
        advanceTimeBy(1_000)
        sdk.pendingLoaded!!()
        advanceTimeBy(500)
        job()
        assertEquals(true, result)
    }

    @Test
    fun `rewarded load fails - false`() = runTest {
        var result: Boolean? = null
        val job = launchLoad { result = it }
        advanceTimeBy(100)
        sdk.pendingFailed!!()
        advanceTimeBy(500)
        job()
        assertEquals(false, result)
    }

    @Test
    fun `rewarded that never answers times out as unavailable`() = runTest {
        var result: Boolean? = null
        val job = launchLoad { result = it }
        advanceTimeBy(AdsPresenterImpl.REWARDED_LOAD_TIMEOUT_MS + 1_000)
        job()
        assertEquals(false, result)
    }

    @Test
    fun `rewarded placement off - unavailable without touching the sdk`() = runTest {
        repo.config.value = AdsConfig(enable = false)
        assertFalse(presenter.loadRewarded(AdPlacement.RewardedBonus))
        assertEquals(emptyList<String>(), sdk.preloads)
    }

    @Test
    fun `rewarded result is passed through, and failures count as not earned`() {
        sdk.ready += AdPlacement.RewardedBonus.id
        var earned: Boolean? = null
        presenter.showRewarded(activity, AdPlacement.RewardedBonus) { earned = it }
        assertEquals(true, earned)

        sdk.rewardEarned = false
        presenter.showRewarded(activity, AdPlacement.RewardedBonus) { earned = it }
        assertEquals(false, earned)

        sdk.throwOnShow = true
        earned = null
        presenter.showRewarded(activity, AdPlacement.RewardedBonus) { earned = it }
        assertEquals(false, earned)
    }

    @Test
    fun `rewarded not loaded - not earned and no show`() {
        var earned: Boolean? = null
        presenter.showRewarded(activity, AdPlacement.RewardedBonus) { earned = it }
        assertEquals(false, earned)
        assertTrue(sdk.shown.isEmpty())
    }

    /** Chạy loadRewarded trong scope của test, trả hàm chờ nó xong. */
    private fun kotlinx.coroutines.test.TestScope.launchLoad(onResult: (Boolean) -> Unit): () -> Unit {
        val job = backgroundScope.launch { onResult(presenter.loadRewarded(AdPlacement.RewardedBonus)) }
        return { testScheduler.advanceUntilIdle(); check(job.isCompleted) }
    }
}
