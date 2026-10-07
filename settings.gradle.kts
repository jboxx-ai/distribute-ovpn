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

rootProject.name = "vpn-kmp-platform"

// Shared KMP modules
include(":shared:api-model")
include(":shared:domain")
include(":shared:crypto")
include(":shared:persistence")
include(":shared:telemetry")
