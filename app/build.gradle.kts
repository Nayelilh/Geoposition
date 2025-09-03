plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")

    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.example.nayeliconstantina"
    compileSdk = 36

    defaultConfig {
        manifestPlaceholders = [ MAPS_API_KEY: project.properties['MAPS_API_KEY'] ?: "" ]
        applicationId = "com.example.nayeliconstantina"
        minSdk = 24
        targetSdk = 36
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions { jvmTarget = "11" }

    buildFeatures { compose = true }
    //composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
}

dependencies {
    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.09.01"))
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Core
    implementation("androidx.core:core-ktx:1.13.1")

    // Google Play Services - Location
    //implementation("com.google.android.gms:play-services-location:21.3.0")
    // Google Maps
    implementation("com.google.android.gms:play-services-maps:18.1.0")
    // Google Location
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("com.google.maps.android:maps-compose:2.11.4")


    // Compose interoperability con Views
    implementation("androidx.compose.ui:ui:1.7.0")
    implementation("androidx.compose.ui:ui-viewbinding:1.7.0")

    // Tests (opcionales)
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.09.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
