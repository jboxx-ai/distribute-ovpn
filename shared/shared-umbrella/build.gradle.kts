plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    jvm()
    macosX64()
    macosArm64()

    listOf(
        macosX64(),
        macosArm64()
    ).forEach { macosTarget ->
        macosTarget.binaries.framework {
            baseName = "VpnCore"
            isStatic = true
            export(project(":shared:api-model"))
            export(project(":shared:domain"))
            export(project(":shared:profile-policy"))
            export(project(":shared:sync"))
            export(project(":shared:crypto"))
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":shared:api-model"))
            api(project(":shared:domain"))
            api(project(":shared:profile-policy"))
            api(project(":shared:sync"))
            api(project(":shared:crypto"))
            api(project(":shared:persistence"))
            api(project(":shared:telemetry"))
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
