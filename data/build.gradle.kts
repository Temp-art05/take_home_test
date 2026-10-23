plugins {
    id("base.android.library")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
}

android {
    namespace = "com.takehome.drama.data"

    testOptions.unitTests.isIncludeAndroidResources = true
}

// Golden test đọc đúng bản fixtures mà flavor dev nhúng vào APK — một bản duy nhất, không copy.
// AGP 9 bỏ DSL `sourceSets.test.resources.srcDir`, nên khai qua Variant API.
androidComponents {
    onVariants { variant ->
        variant.hostTests.values.forEach { test ->
            test.sources.resources?.addStaticSourceDirectory("$rootDir/app/src/dev/assets")
        }
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(project(":domain"))

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.koin.android)
    implementation(libs.bundles.ktor)
    implementation(libs.bundles.room)
    ksp(libs.room.compiler)
    implementation(libs.timber)
    implementation(libs.androidx.datastore.preferences)

    testImplementation(libs.bundles.unit.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}
