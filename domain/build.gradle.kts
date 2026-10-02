plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(libs.kotlinx.datetime)
    implementation(libs.ktor.serialization.kotlinx.json)

    testImplementation(libs.kotlin.test.junit)
}
