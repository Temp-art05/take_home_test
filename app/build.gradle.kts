import com.android.build.api.dsl.ProductFlavor
import java.util.Properties

plugins {
    id("base.android.application")
    id("base.android.compose")
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
}

fun localProperty(key: String): String? = localProperties.getProperty(key)?.takeIf { it.isNotBlank() }

android {
    namespace = "com.takehome.drama"

    defaultConfig {
        applicationId = "com.takehome.drama"
        versionCode = 1
        versionName = "1.0.0"
    }

    androidResources {
        // Chỉ đóng gói locale thực sự hỗ trợ (thay cho resourceConfigurations đã deprecated)
        localeFilters += listOf("en")
    }

    buildFeatures {
        buildConfig = true
    }

    // Keystore + mật khẩu đọc từ local.properties (không commit). Thiếu thì release ra APK unsigned.
    val releaseStoreFile = localProperty("RELEASE_STORE_FILE")?.let(rootProject::file)?.takeIf { it.exists() }
    signingConfigs {
        if (releaseStoreFile != null) {
            create("release") {
                storeFile = releaseStoreFile
                storePassword = localProperty("RELEASE_STORE_PASSWORD")
                keyAlias = localProperty("RELEASE_KEY_ALIAS")
                keyPassword = localProperty("RELEASE_KEY_PASSWORD")
            }
        }
    }

    flavorDimensions += "environment"

    productFlavors {
        create("dev") {
            dimension = "environment"
            // Không có applicationIdSuffix/versionNameSuffix: Firebase chỉ đăng ký một app, dev và prod dùng chung package và versionName.
            buildConfigField("boolean", "IS_DEV_FLAVOR", "true")
            // Dev đọc fixtures trong src/dev/assets: host thật trả 403 nếu bị gọi liên tục.
            buildConfigField("boolean", "USE_FIXTURES", "true")
            stringBuildConfig("BASE_URL", "https://flowdrama.luonluon.com/flowdrama/api/")
        }

        create("prod") {
            dimension = "environment"
            buildConfigField("boolean", "IS_DEV_FLAVOR", "false")
            buildConfigField("boolean", "USE_FIXTURES", "false")
            stringBuildConfig("BASE_URL", "https://flowdrama.luonluon.com/flowdrama/api/")
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            buildConfigField("boolean", "IS_DEBUG", "true")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfigs.findByName("release")?.let { signingConfig = it }
            bundle {
                language {
                    // Giữ mọi ngôn ngữ trong bundle để in-app language picker đổi được runtime
                    enableSplit = false
                }
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("boolean", "IS_DEBUG", "false")
        }
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":data"))
    implementation(project(":core:utils"))
    implementation(project(":core:navigation"))
    implementation(project(":core:analytics"))
    implementation(project(":core:ui-foundation"))
    implementation(project(":core:design-system"))
    implementation(project(":core:media"))
    implementation(project(":feature:onboarding"))
    implementation(project(":feature:main"))
    implementation(project(":feature:watch"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.koin.android)
    implementation(libs.timber)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
}

fun ProductFlavor.stringBuildConfig(name: String, value: String) {
    buildConfigField("String", name, "\"$value\"")
}
