package com.takehome.drama.core.ads

import android.app.Activity
import android.app.Application
import com.takehome.drama.domain.config.AppConfig
import com.takehome.drama.domain.fake.FakeAdsConfigRepository
import com.takehome.drama.domain.model.ads.AdPlacement
import com.takehome.drama.domain.model.ads.AdsConfig
import com.takehome.drama.domain.model.ads.PlacementConfig
import com.takehome.drama.domain.usecase.ads.IsAdPlacementEnabledUseCase
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class AppOpenAdControllerTest {

    private val appConfig = AppConfig("", isDebug = true, isDevFlavor = true, useFixtures = true, versionCode = 1, versionName = "1.0.0")
    private val repo = FakeAdsConfigRepository()
    private val sdk = FakeAdsSdk()
    private val controller = AppOpenAdController(Application(), sdk, IsAdPlacementEnabledUseCase(repo, appConfig))
    private val activity = Activity()
    private val name = AdPlacement.AppOpenResume.id

    @Before
    fun setUp() {
        controller.onActivityVisible(activity)
        controller.onRemoteConfigReady()
    }

    /** Mở app lần đầu (bỏ qua) rồi quay lại từ nền. */
    private fun coldStartThenResume() {
        controller.onAppForeground()
        controller.onAppForeground()
    }

    @Test
    fun `cold start shows nothing, coming back from background shows the ad`() {
        controller.onAppForeground()
        assertEquals(emptyList<String>(), sdk.appOpenShown)
        controller.onAppForeground()
        assertEquals(listOf(name), sdk.appOpenShown)
    }

    @Test
    fun `remote config ready preloads the app open ad`() {
        assertEquals(listOf(name), sdk.preloads)
    }

    @Test
    fun `nothing before remote config has been applied`() {
        val fresh = AppOpenAdController(Application(), sdk, IsAdPlacementEnabledUseCase(repo, appConfig))
        fresh.onActivityVisible(activity)
        fresh.onAppForeground()
        fresh.onAppForeground()
        assertEquals(emptyList<String>(), sdk.appOpenShown)
    }

    @Test
    fun `suppressed resume is skipped once, the next one shows`() {
        controller.onAppForeground()
        controller.suppressNextResume()
        controller.onAppForeground()
        assertEquals(emptyList<String>(), sdk.appOpenShown)
        controller.onAppForeground()
        assertEquals(listOf(name), sdk.appOpenShown)
    }

    @Test
    fun `a video screen blocks it until every blocker is released`() {
        val releasePlayer = controller.block()
        val releaseReels = controller.block()
        coldStartThenResume()
        releasePlayer()
        controller.onAppForeground()
        assertEquals(emptyList<String>(), sdk.appOpenShown)
        releaseReels()
        controller.onAppForeground()
        assertEquals(listOf(name), sdk.appOpenShown)
    }

    @Test
    fun `releasing the same blocker twice does not unblock another screen`() {
        val release = controller.block()
        controller.block()
        release()
        release()
        coldStartThenResume()
        assertEquals(emptyList<String>(), sdk.appOpenShown)
    }

    @Test
    fun `not while another full screen ad is showing`() {
        sdk.fullScreenShowing = true
        coldStartThenResume()
        assertEquals(emptyList<String>(), sdk.appOpenShown)
    }

    @Test
    fun `not within the cooldown after a full screen ad`() {
        sdk.sinceLastFullScreenMs = AppOpenAdController.COOLDOWN_AFTER_FULLSCREEN_MS - 1
        coldStartThenResume()
        assertEquals(emptyList<String>(), sdk.appOpenShown)
        sdk.sinceLastFullScreenMs = AppOpenAdController.COOLDOWN_AFTER_FULLSCREEN_MS
        controller.onAppForeground()
        assertEquals(listOf(name), sdk.appOpenShown)
    }

    @Test
    fun `placement switched off or kill switch - nothing`() {
        repo.config.value = AdsConfig(placements = mapOf(name to PlacementConfig(false)))
        coldStartThenResume()
        repo.config.value = AdsConfig(enable = false)
        controller.onAppForeground()
        assertEquals(emptyList<String>(), sdk.appOpenShown)
    }

    @Test
    fun `no visible activity - nothing`() {
        controller.onActivityHidden(activity)
        coldStartThenResume()
        assertEquals(emptyList<String>(), sdk.appOpenShown)
    }

    @Test
    fun `sdk throwing does not crash`() {
        sdk.throwOnPreload = true
        controller.onRemoteConfigReady()
        coldStartThenResume()
        assertEquals(listOf(name), sdk.appOpenShown)
    }
}
