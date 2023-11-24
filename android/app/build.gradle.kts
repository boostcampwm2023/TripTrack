@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)

    kotlin("kapt")
    alias(libs.plugins.hiltAndroid)
    id("androidx.navigation.safeargs.kotlin")
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")

    kotlin("plugin.serialization") version "1.9.0"
}

android {
    namespace = "com.boostcampwm2023.snappoint"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.boostcampwm2023.snappoint"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures{
        dataBinding = true
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.majorVersion
    }
}

dependencies {

    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.androidx.rules)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)

    //navigation
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    //hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.android.compiler)

    //coil
    implementation(libs.coil)

    //exif
    implementation(libs.androidx.exifinterface)

    //retrofit
    implementation(libs.retrofit)
    //kotlinx.serialization json converter
    implementation(libs.retrofit2.kotlinx.serialization.converter)
    //kotlinx.serialization.json
    implementation(libs.kotlinx.serialization.json)
    //datastore
    implementation(libs.androidx.datastore.preference)

    //room
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)
    implementation(libs.room.ktx)
    kapt(libs.room.compiler)

    //mockk
    testImplementation(libs.mockk)

    //maps
    implementation (libs.play.services.maps)
    //location
    implementation (libs.play.services.location)

    //mockwebserver
    testImplementation(libs.mockwebserver)
    androidTestImplementation(libs.mockwebserver)

    //kotlinx-coroutines-test
    testImplementation(libs.kotlinx.coroutines.test)
}