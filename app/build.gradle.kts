plugins {
    alias(libs.plugins.androidApplication)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.kzmusic"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.kzmusic"
        minSdk = 34
        //noinspection OldTargetApi
        targetSdk = 35
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
}
repositories {
}
dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.preference)
    implementation(files("C:/Users/yaz33/AndroidStudioProjects/Kzmusic/app/libs/spotify-app-remote-release-0.8.0.aar"))
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.gson)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.exoplayer)
    implementation(libs.exoplayer.ui)
    implementation(libs.google.exoplayer.core)
    implementation(libs.exoplayer.extension.mediasession)
    implementation(libs.appcompat)
    //Spotify API
    implementation(libs.auth)
    // Retrofit for API requests and HTTP
    implementation(libs.retrofit)
    implementation(libs.retrofit2.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp3.logging.interceptor)
    implementation(libs.recyclerview)
    // Glide for image loading
    implementation(libs.glide)
    annotationProcessor(libs.glide.compiler)
    //Worker implementation
    implementation(libs.work.runtime)
    //Google firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)
    implementation(libs.play.services.auth)
    implementation(libs.work.runtime.ktx)
    // Room components
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)

// Optional - Kotlin Extensions and Coroutines support
    implementation(libs.room.ktx)

// Optional - Testing
    testImplementation(libs.room.testing)

}