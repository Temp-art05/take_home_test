package com.takehome.drama.navigation

import android.content.Intent
import com.takehome.drama.core.navigation.destination.Destination
import com.takehome.drama.feature.main.navigation.HistoryScreenProvider
import com.takehome.drama.feature.main.navigation.LanguageScreenProvider
import com.takehome.drama.feature.main.navigation.MainScreenProvider
import com.takehome.drama.feature.main.navigation.SavedScreenProvider
import com.takehome.drama.feature.main.navigation.SearchScreenProvider
import com.takehome.drama.feature.onboarding.navigation.OnboardingScreenProvider
import com.takehome.drama.feature.onboarding.navigation.SplashScreenProvider
import com.takehome.drama.feature.watch.navigation.PlayerScreenProvider
import com.takehome.drama.feature.watch.navigation.ShowDetailScreenProvider
import timber.log.Timber

/**
 * Mở thẳng một màn theo mã SCR của design — để chụp màn đặt cạnh mockup (`tools/ui-check`).
 * Chỉ `MainActivity` của flavor dev gọi tới; bản prod không đọc extra này.
 *
 * ```
 * adb shell am start -S -n com.takehome.drama/.MainActivity \
 *     --es debug_screen SCR-0301 --es show_id 12485 --ei episode_index 0
 * ```
 *
 * Sheet (0303/0304) và trạng thái hệ thống (06xx) không phải destination: mở màn cha rồi thao tác tiếp.
 * Extra phụ: `--ei debug_hour 21` ép giờ của Home Day Cycle (MainActivity áp vào `SystemWallClock`),
 * `--es debug_query ceo` mở Search với chữ sẵn,
 * `--es debug_ads_config '{"enable":false}'` thay `ads_config` của Remote Config trong phiên (thử kill switch
 * không cần Console; JSON hỏng → default nhúng).
 */
object DebugScreenLauncher {

    const val EXTRA_SCREEN = "debug_screen"
    const val EXTRA_SHOW_ID = "show_id"
    const val EXTRA_EPISODE_INDEX = "episode_index"
    const val EXTRA_HOUR = "debug_hour"
    const val EXTRA_QUERY = "debug_query"
    const val EXTRA_ADS_CONFIG = "debug_ads_config"

    private const val DEFAULT_SHOW_ID = "12485"

    fun startDestination(intent: Intent?): Destination? {
        val code = intent?.getStringExtra(EXTRA_SCREEN) ?: return null
        val showId = intent.getStringExtra(EXTRA_SHOW_ID) ?: DEFAULT_SHOW_ID
        val episodeIndex = intent.getIntExtra(EXTRA_EPISODE_INDEX, 0)
        val query = intent.getStringExtra(EXTRA_QUERY)
        return destinationFor(code, showId, episodeIndex, query)
            .also { if (it == null) Timber.w("debug_screen=%s không map được, mở luồng thường", code) }
    }

    /** Giờ ép cho Day Cycle; không có extra → null (giờ thật). */
    fun forcedHour(intent: Intent?): Int? =
        intent?.takeIf { it.hasExtra(EXTRA_HOUR) }?.getIntExtra(EXTRA_HOUR, 0)?.takeIf { it in 0..23 }

    /** JSON `ads_config` ép cho phiên này; không có extra → null (dùng Remote Config). */
    fun adsConfigOverride(intent: Intent?): String? = intent?.getStringExtra(EXTRA_ADS_CONFIG)

    private fun destinationFor(code: String, showId: String, episodeIndex: Int, query: String?): Destination? =
        when (code.uppercase()) {
            "SCR-0001" -> SplashScreenProvider.Splash(holdForDebug = true)
            "SCR-0002" -> OnboardingScreenProvider.Onboarding(startStep = 0)
            "SCR-0003" -> OnboardingScreenProvider.Onboarding(startStep = 1)
            "SCR-0004" -> OnboardingScreenProvider.Onboarding(startStep = 2)
            "SCR-0005" -> OnboardingScreenProvider.Onboarding(startStep = 3)
            "SCR-0101" -> MainScreenProvider.Main(initialTab = "Home")
            "SCR-0203" -> MainScreenProvider.Main(initialTab = "Reels")
            "SCR-0202" -> MainScreenProvider.Main(initialTab = "Browse")
            "SCR-0401" -> MainScreenProvider.Main(initialTab = "Library")
            "SCR-0501" -> MainScreenProvider.Main(initialTab = "Settings")
            "SCR-0201" -> SearchScreenProvider.Search(initialQuery = query)
            "SCR-0402" -> HistoryScreenProvider.History
            "SCR-0403" -> SavedScreenProvider.Saved
            "SCR-0502" -> LanguageScreenProvider.Language
            "SCR-0301" -> ShowDetailScreenProvider.ShowDetail(showId)
            "SCR-0302", "SCR-0303", "SCR-0304", "SCR-0604" ->
                PlayerScreenProvider.Player(showId = showId, episodeIndex = episodeIndex)
            else -> null
        }
}
