plugins {
    id("base.android.feature")
}

android {
    namespace = "com.takehome.drama.feature.onboarding"
}

dependencies {
    testImplementation(libs.bundles.unit.test)
    testImplementation(testFixtures(project(":domain")))
    // SavedStateHandle.toRoute đọc arg qua android.os.Bundle.
    testImplementation(libs.robolectric)
}
