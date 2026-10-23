package com.base.app.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.base.app.core.navigation.core.LocalNavController
import com.base.app.core.navigation.core.applyDestination
import com.base.app.feature.main.navigation.MainScreenProvider
import com.base.app.feature.main.navigation.SettingsScreenProvider

private const val NAV_ANIM_DURATION = 300
private const val NAV_ANIM_OFFSET_FRACTION = 0.1f

/**
 * :app là nơi duy nhất biết đủ mọi feature, nên nó nối các feature với nhau bằng cách
 * truyền destination vào ScreenProvider. Nhờ vậy feature không phụ thuộc lẫn nhau.
 */
@Composable
fun AppNavGraph(navController: NavHostController) {
    CompositionLocalProvider(LocalNavController provides navController) {
        NavHost(
            navController = navController,
            startDestination = MainScreenProvider.Main,
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
            applyDestination(MainScreenProvider())
            applyDestination(SettingsScreenProvider())
        }
    }
}
