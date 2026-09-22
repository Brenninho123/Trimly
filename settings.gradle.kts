pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Trimly"
include(":app")
include(":core")

if (providers.gradleProperty("trimlyDesktop").isPresent) {
    include(":desktop")
}

if (providers.gradleProperty("trimlyIos").isPresent) {
    include(":iosApp")
}
