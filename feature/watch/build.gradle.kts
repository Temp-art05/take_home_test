plugins {
    id("base.android.feature")
}

android {
    namespace = "com.takehome.drama.feature.watch"
}

dependencies {
    implementation(project(":core:ads"))
    implementation(project(":core:media"))

    testImplementation(libs.bundles.unit.test)
    testImplementation(testFixtures(project(":core:ads")))
    testImplementation(testFixtures(project(":core:media")))
    testImplementation(testFixtures(project(":domain")))
    // SavedStateHandle.toRoute đọc arg qua android.os.Bundle.
    testImplementation(libs.robolectric)
}
