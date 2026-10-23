plugins {
    id("base.kotlin.jvm")
    // Fake repository + fixture dùng chung cho test của :domain và các feature (`testFixtures(project(":domain"))`).
    `java-test-fixtures`
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)

    testFixturesImplementation(libs.kotlinx.coroutines.core)
    testFixturesImplementation(libs.kotlinx.coroutines.test)
    testFixturesImplementation(libs.junit)

    testImplementation(libs.bundles.unit.test)
}
