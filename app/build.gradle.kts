import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
    id ("com.google.gms.google-services")

}
val properties = Properties()
rootProject.file("secrets.properties").let {
    if (it.exists()) {
        properties.load(it.inputStream())
    }
}

android {
    namespace = "com.bxt"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.bxt"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        manifestPlaceholders["mapbox_access_token"] = properties.getProperty("MAPBOX_PUBLIC_TOKEN", "")

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
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

secrets {
    propertiesFileName = "secrets.properties"

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
    implementation(libs.androidx.material)

    // ViewModel Compose
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.coil.compose)

    // Call api
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    
    //Navigation
    implementation(libs.androidx.navigation.compose)

    //Data Store
    implementation(libs.androidx.datastore.preferences)

    //Hilt
    implementation(libs.dagger.hilt.android)
    kapt(libs.dagger.hilt.android.compiler)


    // Paging3
    implementation(libs.androidx.paging.runtime)
    implementation(libs.paging.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.kotlinx.serialization.json)

    // Location
    implementation(libs.play.services.location)
//    implementation(libs.play.services.maps)
//    implementation(libs.androidx.lifecycle.viewmodel.ktx)
//    implementation(libs.androidx.lifecycle.livedata.ktx)
//    implementation(libs.androidx.activity.ktx)

    //List image
    implementation(libs.androidx.foundation)

    implementation(libs.lottie.compose)


    implementation (libs.androidx.material.icons.extended)
    // Maps
//    implementation("com.google.maps.android:maps-compose:4.3.3")
//    implementation("com.google.android.gms:play-services-maps:18.2.0")
//    implementation("com.google.android.libraries.places:places:3.4.0")
//    implementation("com.google.android.gms:play-services-location:21.2.0")
//    implementation("com.google.maps.android:maps-compose:2.11.4")
//    implementation("com.google.android.libraries.places:places:3.3.0")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")


    //Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))

    implementation("com.google.firebase:firebase-database-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")

    implementation("com.google.code.gson:gson:2.10.1")
    //Mapbox
    implementation(libs.android)
    implementation(libs.maps.compose)
    implementation(libs.autofill)
    implementation(libs.discover)
    implementation(libs.place.autocomplete)
    implementation(libs.mapbox.search.android)
    implementation(libs.mapbox.search.android.ui)


    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}