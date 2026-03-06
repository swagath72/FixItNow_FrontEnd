plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.simats.fixitnow"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.simats.fixitnow"
        minSdk = 35
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation("androidx.gridlayout:gridlayout:1.0.0")
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.8.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation(libs.okhttp)
    implementation(libs.glide)
    implementation(libs.razorpay)
    implementation(libs.osmdroid)
    annotationProcessor(libs.glide.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}