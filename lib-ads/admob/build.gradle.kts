// Mã nguồn vendored từ lib-ads-android (xem lib-ads/README.md). Bỏ 4 mediation adapter của bản gốc:
// bài test chỉ dùng test ID AdMob, adapter kéo thêm repo Maven lạ (Pangle/Mintegral) + SDK nặng.
plugins {
    id("base.android.library")
}

android {
    namespace = "com.github.dinhn.ads.admob"
}

dependencies {
    api(project(":lib-ads:core"))
    implementation(libs.androidx.core.ktx)
    api(libs.play.services.ads)
}
