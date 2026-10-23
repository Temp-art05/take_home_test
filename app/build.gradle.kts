import com.android.build.api.dsl.ProductFlavor
import java.util.Properties

plugins {
    id("base.android.application")
    id("base.android.compose")
    // Thêm app/google-services.json rồi bật 2 plugin dưới là dùng Firebase thật, không phải sửa code.
    // alias(libs.plugins.google.services)
    // alias(libs.plugins.firebase.crashlytics)
}

val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
}

fun localProperty(key: String): String? = localProperties.getProperty(key)?.takeIf { it.isNotBlank() }

android {
    namespace = "com.base.app"

    defaultConfig {
        applicationId = "com.base.app"
        versionCode = 1
        versionName = "1.0.0"
    }

    androidResources {
        // Chỉ đóng gói locale thực sự hỗ trợ (thay cho resourceConfigurations đã deprecated)
        localeFilters += listOf("en", "vi")
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
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            buildConfigField("boolean", "IS_DEV_FLAVOR", "true")
            // TODO: đổi sang BE thật
            stringBuildConfig("BASE_URL", "https://dummyjson.com/")
        }

        create("prod") {
            dimension = "environment"
            buildConfigField("boolean", "IS_DEV_FLAVOR", "false")
            stringBuildConfig("BASE_URL", "https://dummyjson.com/")
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
    implementation(project(":feature:main"))

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
