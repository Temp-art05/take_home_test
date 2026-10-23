package com.takehome.drama.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.takehome.drama.core.navigation.core.LocalNavController
import com.takehome.drama.core.navigation.core.applyDestination
import com.takehome.drama.core.navigation.destination.Destination
import com.takehome.drama.feature.main.navigation.HistoryScreenProvider
import com.takehome.drama.feature.main.navigation.LanguageScreenProvider
import com.takehome.drama.feature.main.navigation.MainScreenProvider
import com.takehome.drama.feature.main.navigation.SavedScreenProvider
import com.takehome.drama.feature.main.navigation.SearchScreenProvider
import com.takehome.drama.feature.onboarding.navigation.NotificationPromptScreenProvider
import com.takehome.drama.feature.onboarding.navigation.OnboardingLanguageScreenProvider
import com.takehome.drama.feature.onboarding.navigation.SplashScreenProvider
import com.takehome.drama.feature.onboarding.navigation.TastePickerScreenProvider
import com.takehome.drama.feature.onboarding.navigation.ValuePropScreenProvider
import com.takehome.drama.feature.watch.navigation.PlayerScreenProvider
import com.takehome.drama.feature.watch.navigation.ShowDetailScreenProvider

private const val NAV_ANIM_DURATION = 300
private const val NAV_ANIM_OFFSET_FRACTION = 0.1f

/**
 * :app là nơi duy nhất biết đủ mọi feature, nên nó nối các feature với nhau bằng cách
 * truyền destination vào ScreenProvider. Nhờ vậy feature không phụ thuộc lẫn nhau.
 */
@Composable
fun AppNavGraph(
    navController: NavHostController,
    startDestination: Destination = SplashScreenProvider.Splash,
) {
    CompositionLocalProvider(LocalNavController provides navController) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { (it * NAV_ANIM_OFFSET_FRACTION).toInt() },
                    animationSpec = tween(NAV_ANIM_DURATION)
                ) + fadeIn(animationSpec = tween(NAV_ANIM_DURATION))
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { -(it * NAV_ANIM_OFFSET_FRACTION).toInt() },
                    animationSpec = tween(NAV_ANIM_DURATION)
                ) + fadeOut(animationSpec = tween(NAV_ANIM_DURATION))
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { -(it * NAV_ANIM_OFFSET_FRACTION).toInt() },
                    animationSpec = tween(NAV_ANIM_DURATION)
                ) + fadeIn(animationSpec = tween(NAV_ANIM_DURATION))
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { (it * NAV_ANIM_OFFSET_FRACTION).toInt() },
                    animationSpec = tween(NAV_ANIM_DURATION)
                ) + fadeOut(animationSpec = tween(NAV_ANIM_DURATION))
            }
        ) {
            val home = MainScreenProvider.Main()

            // Onboarding — SCR-0001…0005
            applyDestination(SplashScreenProvider(home = home))
            applyDestination(OnboardingLanguageScreenProvider())
            applyDestination(ValuePropScreenProvider())
            applyDestination(TastePickerScreenProvider())
            applyDestination(NotificationPromptScreenProvider(home = home))

            // Main 5 tab + màn mở từ tab
            applyDestination(MainScreenProvider(watchRoutes = AppWatchRoutes))
            applyDestination(SearchScreenProvider(watchRoutes = AppWatchRoutes))
            applyDestination(HistoryScreenProvider(watchRoutes = AppWatchRoutes))
            applyDestination(SavedScreenProvider(watchRoutes = AppWatchRoutes))
            applyDestination(LanguageScreenProvider())

            // Xem phim — SCR-0301…0304
            applyDestination(ShowDetailScreenProvider())
            applyDestination(PlayerScreenProvider())
        }
    }
}
