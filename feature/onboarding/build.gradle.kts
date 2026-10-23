plugins {
    id("base.android.feature")
}

android {
    namespace = "com.takehome.drama.feature.onboarding"
}

dependencies {
    implementation(project(":core:ads"))

    testImplementation(testFixtures(project(":core:ads")))
    testImplementation(libs.bundles.unit.test)
    testImplementation(testFixtures(project(":domain")))
    // SavedStateHandle.toRoute đọc arg qua android.os.Bundle.
    testImplementation(libs.robolectric)
}
