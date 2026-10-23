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
    }
}

rootProject.name = "BaseApp"
include(":app")
include(":domain")
include(":data")
include(":core:utils")
include(":core:navigation")
include(":core:analytics")
include(":core:ui-foundation")
include(":core:design-system")
include(":feature:main")
