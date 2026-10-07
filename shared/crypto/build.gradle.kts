plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    jvm()
    macosX64()
    macosArm64()

    sourceSets {
        commonMain.dependencies {
            api(project(":shared:api-model"))
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
