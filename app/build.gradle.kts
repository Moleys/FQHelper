
plugins {
    id("com.android.application")
    
}

android {
    namespace = "com.xxhy.fqhelper"
    compileSdk = 33
    
    ndkVersion = "27.1.12297006"
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.25.1"
        }
    }
    
    defaultConfig {
        applicationId = "com.xxhy.fqhelper"
        minSdk = 26
        targetSdk = 33
        versionCode = 2
        versionName = "1.1"
        
        vectorDrawables { 
            useSupportLibrary = true
        }
        
        externalNativeBuild {
			cmake {
                arguments.add("-DANDROID_ARM_NEON=TRUE")
                arguments.add("-DANDROID_TOOLCHAIN=clang")

            }
		}
		ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a","x86","x86_64")
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        viewBinding = true
        
    }
    
}

dependencies {
    // Xposed API
    compileOnly(files("libs/api-debug.aar"))
    compileOnly(files("libs/api-annotations.jar"))
    
    // DexKit
    // https://mvnrepository.com/artifact/org.luckypray/dexkit
    implementation("org.luckypray:dexkit:2.0.6")
    
    // Gson
    // https://mvnrepository.com/artifact/com.google.code.gson/gson
    implementation("com.google.code.gson:gson:2.11.0")
    
    // Joor
    // https://mvnrepository.com/artifact/org.jooq/joor
    implementation("org.jooq:joor:0.9.15")
    
    // NanoHttpd
    // https://mvnrepository.com/artifact/org.nanohttpd/nanohttpd
    implementation("org.nanohttpd:nanohttpd:2.3.1")
    
    // OkHttp
    //implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
