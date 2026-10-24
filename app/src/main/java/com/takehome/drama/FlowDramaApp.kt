package com.takehome.drama

import android.app.Application
import android.os.Build
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.takehome.drama.core.ads.AdsInitializer
import com.takehome.drama.core.ads.di.adsModule
import com.takehome.drama.core.analytics.Analytics
import com.takehome.drama.core.analytics.analyticsModule
import com.takehome.drama.core.analytics.remoteconfig.RemoteConfigSource
import com.takehome.drama.core.analytics.remoteconfig.remoteConfigModule
import com.takehome.drama.core.designsystem.component.appImageLoader
import com.takehome.drama.core.media.DeviceTier
import com.takehome.drama.core.media.mediaModule
import com.takehome.drama.core.media.prepareMediaCache
import com.takehome.drama.core.utils.notification.NotificationHelper
import com.takehome.drama.data.di.dataModule
import com.takehome.drama.di.AppScope
import com.takehome.drama.di.appModule
import com.takehome.drama.di.configModule
import com.takehome.drama.feature.main.di.mainModule
import com.takehome.drama.feature.onboarding.di.onboardingModule
import com.takehome.drama.feature.watch.di.watchModule
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import org.koin.android.ext.android.getKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import timber.log.Timber

class FlowDramaApp : Application(), SingletonImageLoader.Factory {

    override fun newImageLoader(context: PlatformContext): ImageLoader = appImageLoader(context)

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        NotificationHelper.createNotificationChannel(this)

        startKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.INFO else Level.NONE)
            androidContext(this@FlowDramaApp)
            modules(
                configModule,
                appModule,
                // Analytics & Crashlytics tự rơi về no-op nếu chưa có google-services.json
                analyticsModule(isDebug = BuildConfig.IS_DEBUG),
                remoteConfigModule,
                dataModule,
                adsModule,
                // Dev: `adb shell run-as com.takehome.drama touch files/force_low_tier` (hoặc force_normal_tier) để ép mức máy.
                mediaModule(tierOverride = ::devTierOverride),
                onboardingModule,
                mainModule,
                watchModule,
            )
        }

        get<Analytics>().apply {
            setUserProperty("app_version", BuildConfig.VERSION_NAME)
            setUserProperty("device_model", Build.MODEL)
        }

        // Không có bước này thì remote config không bao giờ được fetch.
        get<CoroutineScope>(AppScope).launch { get<RemoteConfigSource>().fetchAndSync() }
        // MobileAds init chạy nền; request ad đầu tiên chờ remote config áp xong (tối đa 3 s).
        get<AdsInitializer>().start()
        // Cache video theo phiên: xoá bản của lần mở trước ngay khi app lên.
        get<CoroutineScope>(AppScope).launch(Dispatchers.IO) { getKoin().prepareMediaCache() }
    }

    private fun devTierOverride(): DeviceTier? = when {
        !BuildConfig.IS_DEV_FLAVOR -> null
        File(filesDir, "force_low_tier").exists() -> DeviceTier.LOW
        File(filesDir, "force_normal_tier").exists() -> DeviceTier.NORMAL
        else -> null
    }
}
