// Mã nguồn vendored từ lib-ads-android (xem lib-ads/README.md). Build script viết lại theo convention
// plugin của repo — bản gốc dùng AGP 8 + kotlin-android + kotlinOptions, AGP 9 không nhận.
plugins {
    id("base.android.library")
    id("base.android.compose")
}

android {
    namespace = "com.github.dinhn.ads.core"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.ump)
}
