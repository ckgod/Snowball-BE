plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":domain"))

    api(libs.bundles.exposed)
    api(libs.h2.database)
    api(libs.mariadb.java.client)

    testImplementation(libs.kotlin.test.junit)
}

tasks.test {
    useJUnit()
    testLogging {
        events("passed", "skipped", "failed")
    }
}
