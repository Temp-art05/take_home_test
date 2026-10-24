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
 * Sheet (0303/0304) không phải destination: mở màn cha rồi thao tác tiếp. Trạng thái hệ thống mở màn cha
 * kèm điều kiện: SCR-0601 = Home + catalogue treo 60 s (skeleton), SCR-0602 = Saved sau khi xoá sạch Library,
 * SCR-0603 = Home + lần tải catalogue đầu lỗi mạng (Try again thì có dữ liệu), SCR-0605 = Browse (bấm ＋ lưu).
 *
 * Extra phụ: `--ei debug_hour 21` ép giờ của Home Day Cycle (MainActivity áp vào `SystemWallClock`),
 * `--es debug_query ceo` mở Search với chữ sẵn,
 * `--es debug_ads_config '{"enable":false}'` thay `ads_config` của Remote Config trong phiên (thử kill switch
 * không cần Console; JSON hỏng → default nhúng),
 * `--ez debug_seed_library true` ghi lịch sử + show đã lưu giống mockup 4.1–4.3, `--ez debug_clear_library true`
 * xoá sạch, `--el debug_catalog_delay_ms 5000` / `--ei debug_offline_calls 2` ép catalogue chậm / lỗi mạng,
 * `--es debug_theme light|dark` đặt theme.
 */
object DebugScreenLauncher {

    const val EXTRA_SCREEN = "debug_screen"
    const val EXTRA_SHOW_ID = "show_id"
    const val EXTRA_EPISODE_INDEX = "episode_index"
    const val EXTRA_HOUR = "debug_hour"
    const val EXTRA_QUERY = "debug_query"
    const val EXTRA_ADS_CONFIG = "debug_ads_config"
    const val EXTRA_SEED_LIBRARY = "debug_seed_library"
    const val EXTRA_CLEAR_LIBRARY = "debug_clear_library"
    const val EXTRA_CATALOG_DELAY = "debug_catalog_delay_ms"
    const val EXTRA_OFFLINE_CALLS = "debug_offline_calls"
    const val EXTRA_THEME = "debug_theme"

    /** Thao tác dữ liệu trước khi mở màn — MainActivity thực hiện qua Koin. */
    data class DebugSetup(
        val seedLibrary: Boolean,
        val clearLibrary: Boolean,
        val catalogDelayMs: Long,
        val offlineCalls: Int,
        /** "light" / "dark"; null = giữ nguyên. */
        val theme: String?,
    )

    private const val LOADING_HOLD_MS = 60_000L

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

    /** Điều kiện dữ liệu cho phiên; trạng thái 06xx tự kèm điều kiện của nó nếu extra không ghi đè. */
    fun setup(intent: Intent?): DebugSetup? {
        intent ?: return null
        val code = intent.getStringExtra(EXTRA_SCREEN)?.uppercase()
        return DebugSetup(
            seedLibrary = intent.getBooleanExtra(EXTRA_SEED_LIBRARY, false),
            clearLibrary = intent.getBooleanExtra(EXTRA_CLEAR_LIBRARY, code == "SCR-0602"),
            catalogDelayMs = intent.getLongExtra(EXTRA_CATALOG_DELAY, if (code == "SCR-0601") LOADING_HOLD_MS else 0),
            offlineCalls = intent.getIntExtra(EXTRA_OFFLINE_CALLS, if (code == "SCR-0603") 1 else 0),
            theme = intent.getStringExtra(EXTRA_THEME)?.lowercase(),
        )
    }

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
            "SCR-0601", "SCR-0603" -> MainScreenProvider.Main(initialTab = "Home")
            "SCR-0605" -> MainScreenProvider.Main(initialTab = "Browse")
            "SCR-0602" -> SavedScreenProvider.Saved
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
