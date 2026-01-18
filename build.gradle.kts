plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.ktor) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

group = "com.ckgod"
version = "0.1.8"

subprojects {
    group = rootProject.group
    version = rootProject.version
}
