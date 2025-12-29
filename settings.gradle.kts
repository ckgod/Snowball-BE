rootProject.name = "Snowball"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

include(":snowball-models")
include(":domain")
include(":infrastructure:database")
include(":infrastructure:kis-api")
include(":presentation")
include(":application")
