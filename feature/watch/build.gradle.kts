plugins {
    id("base.android.feature")
}

android {
    namespace = "com.takehome.drama.feature.watch"
}

dependencies {
    implementation(project(":core:media"))

    testImplementation(libs.bundles.unit.test)
    // SavedStateHandle.toRoute đọc arg qua android.os.Bundle.
    testImplementation(libs.robolectric)
}
