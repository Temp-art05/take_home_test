plugins {
    id("base.android.library")
    id("base.android.compose")
}

android {
    namespace = "com.takehome.drama.core.designsystem"
}

dependencies {
    implementation(libs.androidx.core.ktx)

    api(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.coil.gif)
}
