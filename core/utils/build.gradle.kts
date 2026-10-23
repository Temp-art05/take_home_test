plugins {
    id("base.android.library")
}

android {
    namespace = "com.takehome.drama.core.utils"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.timber)
}
