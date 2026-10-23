plugins {
    id("base.android.feature")
}

android {
    namespace = "com.takehome.drama.feature.main"
}

dependencies {
    implementation(project(":core:media"))

    testImplementation(libs.bundles.unit.test)
    testImplementation(testFixtures(project(":domain")))
    testImplementation(testFixtures(project(":core:media")))
    // SavedStateHandle.toRoute đọc arg qua android.os.Bundle.
    testImplementation(libs.robolectric)
}
