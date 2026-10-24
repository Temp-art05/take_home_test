pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    // Máy chưa có JDK 17 thì Gradle tự tải về (toolchain khai ở build-logic/ProjectExt.kt).
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Lib ads của tác giả (lib-ads-android) phát hành qua GitHub Packages — token đọc ở gradle.properties.
        maven {
            url = uri("https://maven.pkg.github.com/lurdsoft/lib-ads-android")
            credentials {
                username = providers.gradleProperty("gpr.user").orNull ?: System.getenv("GITHUB_ACTOR")
                password = providers.gradleProperty("gpr.key").orNull ?: System.getenv("GITHUB_TOKEN")
            }
            content { includeGroup("com.github.dinhn") }
        }
        // ads-admob kéo theo mediation adapter Pangle + Mintegral (như HomeMorph) — SDK của họ chỉ có ở repo riêng.
        maven {
            url = uri("https://artifact.bytedance.com/repository/pangle")
            content { includeGroupByRegex("com\\.pangle.*") }
        }
        maven {
            url = uri("https://dl-maven-android.mintegral.com/repository/mbridge_android_sdk_oversea")
            content { includeGroupByRegex("com\\.mbridge.*") }
        }
    }
}

rootProject.name = "FlowDrama"
include(":app")
include(":domain")
include(":data")
include(":core:utils")
include(":core:navigation")
include(":core:analytics")
include(":core:ui-foundation")
include(":core:design-system")
include(":core:media")
include(":core:ads")
include(":feature:onboarding")
include(":feature:main")
include(":feature:watch")
