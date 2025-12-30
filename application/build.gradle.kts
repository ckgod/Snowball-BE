plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.serialization)
}

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":infrastructure:database"))
    implementation(project(":infrastructure:kis-api"))
    implementation(project(":presentation"))

    implementation(libs.bundles.ktor.server)
    implementation(libs.ktor.serialization.kotlinx.json)

    implementation(libs.bundles.ktor.client)

    implementation(libs.logback.classic)

    implementation(libs.koin.core)
    implementation(libs.koin.ktor)
    implementation(libs.koin.logger.slf4j)

    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)
}
