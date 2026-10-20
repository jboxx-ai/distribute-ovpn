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

rootProject.name = "distribute-ovpn"

// Shared KMP modules
include(":shared:api-model")
include(":shared:domain")
include(":shared:profile-policy")
include(":shared:crypto")
include(":shared:persistence")
include(":shared:telemetry")
include(":shared:sync")
include(":shared:shared-umbrella")
include(":backend:registry-server")
include(":backend:admin-cli")
include(":apps:android")
