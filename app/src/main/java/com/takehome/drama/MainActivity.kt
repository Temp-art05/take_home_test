package com.takehome.drama

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.takehome.drama.core.ads.config.AdsDebugOverride
import com.takehome.drama.core.designsystem.theme.AppTheme
import com.takehome.drama.data.remote.datasource.DebugCatalogFault
import com.takehome.drama.data.util.SystemWallClock
import com.takehome.drama.di.AppScope
import com.takehome.drama.di.ThemeHolder
import com.takehome.drama.domain.model.ThemeMode
import com.takehome.drama.domain.usecase.discovery.WallClock
import com.takehome.drama.domain.usecase.settings.SetThemeUseCase
import com.takehome.drama.navigation.AppNavGraph
import com.takehome.drama.navigation.DebugLibrarySeeder
import com.takehome.drama.navigation.DebugScreenLauncher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get

/**
 * Dùng AppCompatActivity (không phải ComponentActivity) vì in-app language picker gọi
 * AppCompatDelegate.setApplicationLocales() — API 33+ thì hệ thống lo, dưới 33 thì
 * AppCompat lo, và nó cần activity là AppCompat.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // enableEdgeToEdge() đã tự gọi setDecorFitsSystemWindows(false) — không gọi lại.
        // Cũng không ép immersive/hide navigation bar: đó là quyết định của từng app,
        // không phải mặc định của base.
        enableEdgeToEdge()

        // Chỉ bản dev, và chỉ lần mở đầu (không phải recreate): mở thẳng màn theo mã SCR để chụp so mockup.
        val debugStart = if (BuildConfig.IS_DEV_FLAVOR && savedInstanceState == null) {
            DebugScreenLauncher.startDestination(intent)
        } else {
            null
        }
        if (BuildConfig.IS_DEV_FLAVOR) {
            DebugScreenLauncher.forcedHour(intent)?.let { (get<WallClock>() as? SystemWallClock)?.overrideHour = it }
            DebugScreenLauncher.adsConfigOverride(intent)?.let { get<AdsDebugOverride>().set(it) }
            if (savedInstanceState == null) DebugScreenLauncher.setup(intent)?.let(::applyDebugSetup)
        }

        val themeHolder = get<ThemeHolder>()
        setContent {
            val theme by themeHolder.theme.collectAsStateWithLifecycle()
            AppTheme(darkTheme = theme == ThemeMode.Dark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    if (debugStart != null) {
                        AppNavGraph(navController = navController, startDestination = debugStart)
                    } else {
                        AppNavGraph(navController = navController)
                    }
                }
            }
        }
    }

    /** Chỉ flavor dev — điều kiện dữ liệu để chụp Library / trạng thái hệ thống (xem [DebugScreenLauncher]). */
    private fun applyDebugSetup(setup: DebugScreenLauncher.DebugSetup) {
        get<DebugCatalogFault>().apply {
            delayMs = setup.catalogDelayMs
            offlineCalls = setup.offlineCalls
        }
        val seeder = DebugLibrarySeeder(get(), get())
        val setTheme = get<SetThemeUseCase>()
        get<CoroutineScope>(AppScope).launch {
            if (setup.clearLibrary) seeder.clear()
            if (setup.seedLibrary) seeder.seed(System.currentTimeMillis())
            when (setup.theme) {
                "light" -> setTheme(ThemeMode.Light)
                "dark" -> setTheme(ThemeMode.Dark)
            }
        }
    }
}
