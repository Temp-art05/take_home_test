package com.takehome.drama.domain.usecase.ads

import com.takehome.drama.domain.model.ads.AdPlacement
import com.takehome.drama.domain.model.ads.AdsConfig
import com.takehome.drama.domain.model.ads.PlacementConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class AdsPolicyTest {

    private val version = "1.0.0"

    private fun decide(config: AdsConfig, placement: AdPlacement = AdPlacement.NativeHome, versionName: String = version) =
        AdsPolicy.decide(config, versionName, placement)

    @Test
    fun `global switch off wins over a placement that is explicitly on`() {
        val config = AdsConfig(enable = false, placements = mapOf("native_home" to PlacementConfig(enable = true)))
        assertEquals(AdsDecision.Disabled(AdsDecision.Reason.GlobalOff), decide(config))
    }

    @Test
    fun `blocked version wins over a placement that is explicitly on`() {
        // Phép thử 2 của đề: enable=true, version bị chặn, placement bật → không ad nào.
        val config = AdsConfig(
            enable = true,
            disabledVersions = listOf("1.0.0"),
            placements = mapOf("native_home" to PlacementConfig(enable = true)),
        )
        AdPlacement.entries.forEach { placement ->
            assertEquals(AdsDecision.Disabled(AdsDecision.Reason.VersionBlocked), decide(config, placement))
        }
    }

    @Test
    fun `placement off only turns that placement off`() {
        val config = AdsConfig(placements = mapOf("native_home" to PlacementConfig(enable = false)))
        assertEquals(AdsDecision.Disabled(AdsDecision.Reason.PlacementOff), decide(config, AdPlacement.NativeHome))
        assertEquals(AdsDecision.Enabled, decide(config, AdPlacement.InterstitialOnboarding))
        assertEquals(AdsDecision.Enabled, decide(config, AdPlacement.RewardedBonus))
    }

    @Test
    fun `placement missing from the config uses the code default`() {
        val config = AdsConfig(placements = mapOf("native_home" to PlacementConfig(enable = false)))
        AdPlacement.entries.filter { it != AdPlacement.NativeHome }.forEach { placement ->
            val expected = if (placement.defaultEnabled) AdsDecision.Enabled else AdsDecision.Disabled(AdsDecision.Reason.PlacementOff)
            assertEquals(expected, decide(config, placement))
        }
    }

    @Test
    fun `version match is exact - near misses do not block`() {
        listOf("1.0", "1.0.0 ", " 1.0.0", "1.0.0-dev", "V1.0.0", "1.0.00").forEach { listed ->
            val config = AdsConfig(disabledVersions = listOf(listed))
            assertEquals("'$listed' must not block 1.0.0", AdsDecision.Enabled, decide(config))
        }
    }

    @Test
    fun `other versions in the list do not block this one`() {
        val config = AdsConfig(disabledVersions = listOf("0.9.0", "1.0.1"))
        assertEquals(AdsDecision.Enabled, decide(config))
        assertEquals(AdsDecision.Disabled(AdsDecision.Reason.VersionBlocked), decide(config, versionName = "1.0.1"))
    }

    @Test
    fun `embedded default turns every placement on`() {
        AdPlacement.entries.forEach { placement ->
            assertEquals(AdsDecision.Enabled, decide(AdsConfig.DEFAULT, placement))
        }
    }

    @Test
    fun `empty disabled versions blocks nothing`() {
        assertEquals(AdsDecision.Enabled, decide(AdsConfig(disabledVersions = emptyList())))
    }

    @Test
    fun `isEnabled mirrors the decision`() {
        assertEquals(true, AdsPolicy.isEnabled(AdsConfig.DEFAULT, version, AdPlacement.RewardedBonus))
        assertEquals(false, AdsPolicy.isEnabled(AdsConfig(enable = false), version, AdPlacement.RewardedBonus))
    }

    @Test
    fun `placement ids are the ones documented for the console`() {
        assertEquals(
            listOf("interstitial_onboarding", "native_home", "rewarded_bonus"),
            AdPlacement.entries.map { it.id },
        )
    }
}
