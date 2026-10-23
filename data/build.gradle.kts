plugins {
    id("base.android.library")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
}

android {
    namespace = "com.base.app.data"
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
}
