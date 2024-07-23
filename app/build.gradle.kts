plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.denise.castro.adicionaprodutos"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.denise.castro.adicionaprodutos"
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures{
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    //Color picker
    implementation(libs.colorpickerview)

    //Lifecycle
    implementation(libs.androidx.lifecycle.runtime.ktx)

    //Coroutines for firebase
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)

    //Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.play.services.cast.framework)

    //Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}