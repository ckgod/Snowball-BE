plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.ktor) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

group = "com.ckgod"
version = "0.2.0"

subprojects {
    group = rootProject.group
    version = rootProject.version
}
