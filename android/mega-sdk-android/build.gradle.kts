plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "nz.mega.sdk"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        
        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++17"
            }
        }
    }

    sourceSets.getByName("main") {
        java.srcDirs("../app/src/main/cpp/mega-sdk/bindings/java")
        java.exclude("**/MegaApiSwing.java")
    }

    externalNativeBuild {
        cmake {
            path = file("../app/src/main/cpp/mega-sdk/CMakeLists.txt")
            version = "3.22.1"
        }
    }
}

dependencies {
}
