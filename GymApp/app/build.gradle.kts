plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.photoviewer"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.photoviewer"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Default API URL for debug builds (localhost via Android emulator)
        buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8000/\"")
    }

    buildTypes {
        debug {
            // Development: localhost via Android emulator
            buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8000/\"")
        }
        create("staging") {
            // Testing: PythonAnywhere (debuggable)
            initWith(getByName("debug"))
            buildConfigField("String", "API_BASE_URL", "\"https://mouseku.pythonanywhere.com/\"")
            applicationIdSuffix = ".staging"
            isDebuggable = true
        }
        release {
            // Production: PythonAnywhere
            buildConfigField("String", "API_BASE_URL", "\"https://mouseku.pythonanywhere.com/\"")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    buildFeatures {
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("androidx.work:work-runtime:2.9.0")
    implementation(libs.mpandroidchart)
    testImplementation(libs.junit)
    testImplementation("org.mockito:mockito-core:5.14.2")
    testImplementation("org.json:json:20240303")
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}