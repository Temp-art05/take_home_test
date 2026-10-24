plugins {
    id("base.android.library")
    id("base.android.compose")
}

android {
    namespace = "com.takehome.drama.core.designsystem"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    // LocalLifecycleOwner — Reduce Motion đọc lại mỗi ON_RESUME.
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.compose.material.icons.core)

    api(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.coil.gif)
}
