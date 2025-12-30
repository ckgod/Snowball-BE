plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(project(":snowball-models"))
    implementation(project(":domain"))

    implementation(libs.bundles.ktor.server)
    implementation(libs.ktor.serialization.kotlinx.json)
}
