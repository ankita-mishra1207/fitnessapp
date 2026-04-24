plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.secrets)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.fitnessapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.fitnessapp"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"




































































































































































        
        buildConfigField("String", "GEMINI_API_KEY", "\"AIzaSyATGSUhGmZGtSP5offtUJNiWKmn8e0UQ20\"")
        buildConfigField("String", "YOUTUBE_API_KEY", "\"AIzaSyDyH-v0ACy8VSTSrW1PBYHZh4Ij9IYQ148\"")
        buildConfigField("String", "USDA_API_KEY", "\"tSxwnLIEDzhwaRyPBgfAjYvweiqXTt9qbXiXgm4q\"")
        buildConfigField("String", "SPOONACULAR_API_KEY", "\"e29bbc92d45745aca3361cabdd20d27a\"")
        buildConfigField("String", "SUPABASE_URL", "\"https://qwiseuymgtftcdpstfzn.supabase.co\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"sb_publishable_OWL-_AjdckpY5DRuwJIbrg_MZUm7aT_\"")
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    ksp {
        arg("room.generateKotlin", "true")
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
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("io.github.jan-tennert.supabase:auth-kt:2.5.0")
    implementation("io.github.jan-tennert.supabase:postgrest-kt:2.5.0")
    implementation("io.github.jan-tennert.supabase:gotrue-kt:2.5.0")
    implementation("io.github.jan-tennert.supabase:supabase-kt:2.5.0")
    implementation("io.github.jan-tennert.supabase:postgrest-kt:2.5.0")

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // CameraX and ML Kit
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.barcode.scanning)
    implementation("com.google.mlkit:image-labeling:17.0.7")
    implementation("com.google.guava:guava:33.3.1-android")
    implementation("com.google.guava:listenablefuture:1.0")

    // Health Connect
    implementation("androidx.health.connect:connect-client:1.1.0-alpha11")

    // Location
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // Gemini AI
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")

    // Google Auth (Keeping play-services-auth for ID Token)
    implementation(libs.play.services.auth)

    // Supabase
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.auth)
    implementation(libs.supabase.postgrest)
    implementation(libs.ktor.client.android)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}