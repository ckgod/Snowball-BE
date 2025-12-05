plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(project(":common"))
    implementation(project(":domain"))

    implementation(libs.bundles.exposed)
    implementation(libs.h2.database)

    implementation(libs.bundles.ktor.client)
    implementation(libs.ktor.serialization.kotlinx.json)
}
