plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "es.chiteroman.playintegrityfix"
    compileSdk = 35
    buildToolsVersion = "35.0.0"
    ndkVersion = "27.2.12479018"

    buildFeatures {
        prefab = true
    }

    packaging {
        jniLibs {
            excludes += "**/libdobby.so"
        }
        resources {
            excludes += "**"
        }
    }

    defaultConfig {
        applicationId = "es.chiteroman.playintegrityfix"
        minSdk = 26
        targetSdk = 35
        versionCode = 18400
        versionName = "v18.4"
        multiDexEnabled = false

        externalNativeBuild {
            cmake {
                abiFilters(
                    "arm64-v8a",
                    "armeabi-v7a"
                )

                arguments(
                    "-DCMAKE_BUILD_TYPE=MinSizeRel",
                    "-DANDROID_STL=none",
                    "-DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON",
                    "-DANDROID_ALLOW_UNDEFINED_SYMBOLS=ON",
                    "-DCMAKE_CXX_STANDARD=23",
                    "-DCMAKE_C_STANDARD=23",
                    "-DCMAKE_INTERPROCEDURAL_OPTIMIZATION=ON",
                    "-DCMAKE_VISIBILITY_INLINES_HIDDEN=ON",
                    "-DCMAKE_CXX_VISIBILITY_PRESET=hidden",
                    "-DCMAKE_C_VISIBILITY_PRESET=hidden",
                    "-DCMAKE_ANDROID_EXCEPTIONS=OFF",
                    "-DCMAKE_ANDROID_RTTI=OFF",
                    "-DANDROID_CPP_FEATURES='no-rtti no-exceptions'"
                )

                cFlags(
                    "-std=c23",
                    "-fvisibility=hidden",
                    "-fvisibility-inlines-hidden"
                )

                cppFlags(
                    "-std=c++23",
                    "-fno-exceptions",
                    "-fno-rtti",
                    "-fvisibility=hidden",
                    "-fvisibility-inlines-hidden"
                )
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            multiDexEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.31.1"
        }
    }
}

dependencies {
    implementation(libs.cxx)
    implementation(libs.hiddenapibypass)
}

tasks.register("updateModuleProp") {
    doLast {
        val versionName = project.android.defaultConfig.versionName
        val versionCode = project.android.defaultConfig.versionCode

        val modulePropFile = project.rootDir.resolve("module/module.prop")

        var content = modulePropFile.readText()

        content = content.replace(Regex("version=.*"), "version=$versionName")
        content = content.replace(Regex("versionCode=.*"), "versionCode=$versionCode")

        modulePropFile.writeText(content)
    }
}

tasks.register("copyFiles") {
    dependsOn("updateModuleProp")

    doLast {
        val moduleFolder = project.rootDir.resolve("module")
        val dexFile =
            project.layout.buildDirectory.get().asFile.resolve("intermediates/dex/release/minifyReleaseWithR8/classes.dex")
        val soDir =
            project.layout.buildDirectory.get().asFile.resolve("intermediates/stripped_native_libs/release/stripReleaseDebugSymbols/out/lib")

        dexFile.copyTo(moduleFolder.resolve("classes.dex"), overwrite = true)

        soDir.walk().filter { it.isFile && it.extension == "so" }.forEach { soFile ->
            val abiFolder = soFile.parentFile.name
            val destination = moduleFolder.resolve("zygisk/$abiFolder.so")
            soFile.copyTo(destination, overwrite = true)
        }
    }
}

tasks.register<Zip>("zip") {
    dependsOn("copyFiles")

    archiveFileName.set("PlayIntegrityFix_${project.android.defaultConfig.versionName}.zip")
    destinationDirectory.set(project.rootDir.resolve("out"))

    from(project.rootDir.resolve("module"))
}

afterEvaluate {
    tasks["assembleRelease"].finalizedBy("updateModuleProp", "copyFiles", "zip")
}
