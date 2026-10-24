package com.takehome.drama.domain.usecase.ads

import app.cash.turbine.test
import com.takehome.drama.domain.config.AppConfig
import com.takehome.drama.domain.fake.FakeAdsConfigRepository
import com.takehome.drama.domain.model.ads.AdPlacement
import com.takehome.drama.domain.model.ads.AdsConfig
import com.takehome.drama.domain.model.ads.PlacementConfig
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdPlacementUseCaseTest {

    private fun appConfig(versionName: String) = AppConfig(
        baseUrl = "",
        isDebug = true,
        isDevFlavor = true,
        useFixtures = true,
        versionCode = 1,
        versionName = versionName,
    )

    @Test
    fun `a config fetched late still reaches the placement`() = runTest {
        val repo = FakeAdsConfigRepository()
        val observe = ObserveAdPlacementUseCase(repo, appConfig("1.0.0"))

        observe(AdPlacement.NativeHome).test {
            assertTrue(awaitItem())
            repo.config.value = AdsConfig(placements = mapOf("native_home" to PlacementConfig(enable = false)))
            assertFalse(awaitItem())
            // Cấu hình đổi ở placement khác không làm phát lại giá trị cũ.
            repo.config.value = AdsConfig(placements = mapOf("native_home" to PlacementConfig(enable = false), "rewarded_bonus" to PlacementConfig(false)))
            repo.config.value = AdsConfig()
            assertTrue(awaitItem())
        }
    }

    @Test
    fun `version comes from AppConfig`() = runTest {
        val repo = FakeAdsConfigRepository(AdsConfig(disabledVersions = listOf("2.0.0")))
        observeFirst(ObserveAdPlacementUseCase(repo, appConfig("2.0.0")), expected = false)
        observeFirst(ObserveAdPlacementUseCase(repo, appConfig("1.0.0")), expected = true)
    }

    @Test
    fun `point check reads the current config`() {
        val repo = FakeAdsConfigRepository()
        val isEnabled = IsAdPlacementEnabledUseCase(repo, appConfig("1.0.0"))
        assertTrue(isEnabled(AdPlacement.RewardedBonus))
        repo.config.value = AdsConfig(enable = false)
        assertFalse(isEnabled(AdPlacement.RewardedBonus))
    }

    private suspend fun observeFirst(useCase: ObserveAdPlacementUseCase, expected: Boolean) {
        useCase(AdPlacement.InterstitialOnboarding).test {
            assertEquals(expected, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
