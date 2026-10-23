package com.takehome.drama

import android.app.Application
import android.os.Build
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.takehome.drama.core.analytics.Analytics
import com.takehome.drama.core.analytics.analyticsModule
import com.takehome.drama.core.analytics.remoteconfig.RemoteConfigSource
import com.takehome.drama.core.analytics.remoteconfig.remoteConfigModule
import com.takehome.drama.core.designsystem.component.appImageLoader
import com.takehome.drama.core.navigation.core.skeletonModule
import com.takehome.drama.core.utils.notification.NotificationHelper
import com.takehome.drama.data.di.dataModule
import com.takehome.drama.di.AppScope
import com.takehome.drama.di.appModule
import com.takehome.drama.di.configModule
import com.takehome.drama.feature.main.di.mainModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
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
                skeletonModule,
                mainModule,
            )
        }

        get<Analytics>().apply {
            setUserProperty("app_version", BuildConfig.VERSION_NAME)
            setUserProperty("device_model", Build.MODEL)
        }

        // Không có bước này thì remote config không bao giờ được fetch.
        get<CoroutineScope>(AppScope).launch { get<RemoteConfigSource>().fetchAndSync() }
    }
}
