plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.e_sentinel"
    compileSdk = 36
    ndkVersion = "29.0.14033849 rc4"

    packagingOptions {
        jniLibs {
            useLegacyPackaging = false
        }
    }

    packaging {
        resources {
            excludes += "META-INF/DEPENDENCIES"
        }
    }

    defaultConfig {
        applicationId = "com.example.e_sentinel"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "GOOGLE_MAPS_API_KEY", "\"YOUR_API_KEY\"")
        buildConfigField("String", "NEWS_API_KEY", "\"YOUR_API_KEY\"")
        buildConfigField("String", "TWILIO_ACCOUNT_SID", "\"YOUR_API_KEY\"")
        buildConfigField("String", "TWILIO_AUTH_TOKEN", "\"YOUR_API_KEY\"")
        buildConfigField("String", "OPEN_WEATHER_MAP_API_KEY", "\"YOUR_API_KEY\"")
        buildConfigField("String", "TWILIO_SMS_NUMBER", "\"YOUR_TWILIO_SMS_NUMBER\"") // Twilio SMS number
        buildConfigField("String", "TWILIO_WHATSAPP_NUMBER", "\"whatsapp:YOUR_TWILIO_WHATSAPP_NUMBER\"") // Twilio whatsapp number
        buildConfigField("String", "ALERT_PHONE_NUMBER", "\"YOUR_ALERT_PHONE_NUMBER\"") // Emergency contact
        buildConfigField("String", "Whatsapp_ALERT_PHONE_NUMBER", "\"whatsapp:YOUR_WHATSAPP_NUMBER\"") // For sending SOS through Whatsapp


        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            ndk.debugSymbolLevel = "FULL"
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
    sourceSets {
        getByName("main") {
            assets.srcDirs(
                File("../models/src/main/assets"),
                File("../models/build/generated/assets")
            )
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    // Add these for core Android functionality
    implementation("androidx.core:core-ktx:1.13.1") // For Kotlin extensions and basic utils
    implementation("androidx.appcompat:appcompat:1.7.0") // For AppCompatActivity and compatibility
    implementation("androidx.activity:activity-ktx:1.9.2") // For Activity Kotlin extensions (helps with onCreate, etc.)
    implementation("androidx.constraintlayout:constraintlayout:2.1.4") // If using ConstraintLayout in XML; add if needed
    implementation("com.google.android.material:material:1.12.0") // For Material Design components (buttons, themes)
    implementation("com.twilio.sdk:twilio:9.13.1")

    // If testing, keep these
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
    implementation("com.google.android.gms:play-services-maps:18.1.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("androidx.activity:activity-ktx:1.8.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    implementation("androidx.work:work-runtime-ktx:2.8.1") // for background retries / offline queue
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.json:json:20231013") // For JSON parsing

    //vosk
    implementation(project(":models"))
    implementation(group = "com.alphacephei", name = "vosk-android", version = "0.3.70")
}
