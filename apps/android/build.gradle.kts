plugins {
    alias(libs.plugins.android.application)
    kotlin("android")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "fyi.rifqifardi.vpn.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "fyi.rifqifardi.vpn.android"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(project(":shared:api-model"))
    implementation(project(":shared:domain"))
    implementation(project(":shared:profile-policy"))
    implementation(project(":shared:crypto"))
    implementation(project(":shared:sync"))
    implementation(project(":shared:persistence"))
    implementation(project(":shared:telemetry"))

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    testImplementation(libs.kotlin.test)
}
