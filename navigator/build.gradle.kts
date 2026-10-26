plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.parcelize")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("com.google.firebase.crashlytics")
    id("com.google.gms.google-services")
}

android {
    namespace = "pl.org.seva.navigator"
    compileSdk = 37

    defaultConfig {
        applicationId = "pl.org.seva.navigator"
        minSdk = 25
        targetSdk = 28  // Upgrading above 28 requires a change in location permissions
                        // https://developer.android.com/training/location/background
                        // It also requires permission ACTIVITY_RECOGNITION
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
        versionCode = 48
        versionName = "2.8.0"
        androidResources {
            localeFilters += listOf("en", "pl")
        }
    }

    buildFeatures {
        viewBinding = true
        compose = true
    }

    sourceSets {
        named("main") {
            kotlin.directories.add("src/main/kotlin")
        }
        named("test") {
            kotlin.directories.add("src/test/kotlin")
        }
        named("androidTest") {
            kotlin.directories.add("src/androidTest/kotlin")
        }
        named("debug") {
            kotlin.directories.add("src/debug/kotlin")
        }
        named("release") {
            kotlin.directories.add("src/release/kotlin")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            isCrunchPngs = true
        }
    }

    packaging {
        resources {
            excludes += listOf(
                "META-INF/rxjava.properties",
                "META-INF/main.kotlin_module",
                "META-INF/atomicfu.kotlin_module"
            )
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    lint {
        checkReleaseBuilds = false
        checkOnly += "Interoperability"
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

    // Kotlin, RxKotlin
    implementation("io.reactivex.rxjava2:rxkotlin:2.4.0")
    implementation("io.reactivex.rxjava2:rxandroid:2.1.1")
    implementation("org.kodein.di:kodein-di:7.3.0")

    // Jetpack, AndroidX
    implementation("androidx.core:core-ktx:1.5.0-alpha02")
    implementation("androidx.lifecycle:lifecycle-extensions:2.2.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.2.0")
    ksp("androidx.room:room-compiler:2.8.5")
    implementation("androidx.room:room-runtime:2.8.5")
    implementation("androidx.room:room-ktx:2.8.5")
    testImplementation("androidx.room:room-testing:2.8.5")
    implementation("androidx.navigation:navigation-fragment-ktx:2.3.0")
    implementation("androidx.navigation:navigation-ui-ktx:2.3.0")
    implementation("com.google.android.material:material:1.3.0-alpha02")
    implementation(platform("com.google.firebase:firebase-bom:28.4.2"))
    implementation("com.google.firebase:firebase-analytics-ktx")

    // Play, Firebase
    implementation("com.google.android.gms:play-services-maps:17.0.0")
    implementation("com.google.android.gms:play-services-location:17.0.0")
    implementation("com.google.android.gms:play-services-auth:18.1.0")
    implementation("com.google.firebase:firebase-core:17.5.0")
    implementation("com.google.firebase:firebase-auth:19.3.2")
    implementation("com.google.firebase:firebase-database:19.3.1")
    implementation("com.google.firebase:firebase-crashlytics:17.2.1")

    // Jetpack Compose
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("com.google.maps.android:maps-compose:4.4.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Testing
    androidTestImplementation("org.mockito:mockito-android:3.2.4")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.3.0-alpha03") {
        exclude(group = "com.android.support", module = "support-annotations")
    }
    testImplementation("junit:junit:4.13")
}

googleServices { disableVersionCheck = true }
