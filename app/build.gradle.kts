plugins {
    alias(libs.plugins.androidApplication)
}

android {
    namespace = "com.example.kzmusic"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.kzmusic"
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
}
repositories {
}
dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.preference)
    implementation(files("C:\\Users\\yaz33\\AndroidStudioProjects\\Kzmusic\\spotify-app-remote-release-0.8.0.aar"))
    implementation(files("C:\\Users\\yaz33\\AndroidStudioProjects\\Kzmusic\\app\\libs\\TarsosDSP-Android-latest.jar"))
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("com.google.android.gms:play-services-auth:21.1.0")
    implementation("com.google.code.gson:gson:2.8.6")
    implementation("com.google.android.material:material:1.4.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.")
    implementation("com.google.android.exoplayer:exoplayer:2.19.1")
    implementation("com.google.android.exoplayer:exoplayer-ui:2.19.1")
    implementation("com.google.android.exoplayer:exoplayer-core:2.19.1")
    implementation("com.google.android.exoplayer:extension-mediasession:2.19.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    //Spotify API
    implementation("com.spotify.android:auth:1.2.5")
    // Retrofit for API requests and HTTP
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.9.1")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.1")
    implementation("androidx.recyclerview:recyclerview:1.2.1")
    // Glide for image loading
    implementation("com.github.bumptech.glide:glide:4.12.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.12.0")
    // CameraX dependencies
    implementation("androidx.camera:camera-core:1.1.0")
    implementation("androidx.camera:camera-camera2:1.1.0")
    implementation("androidx.camera:camera-lifecycle:1.1.0")
    implementation("androidx.camera:camera-view:1.1.0")
    // Use this dependency to use the dynamically downloaded model in Google Play Services
    implementation("com.google.android.gms:play-services-mlkit-face-detection:17.1.0")

}