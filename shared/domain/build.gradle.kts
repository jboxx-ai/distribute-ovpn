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
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
