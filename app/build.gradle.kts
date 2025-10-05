plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
}
import java.util.Properties
import java.io.FileInputStream

android {
    namespace = "com.bravebrain"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.bravebrain"
        minSdk = 24
        targetSdk = 36
        versionCode = 21
        versionName = "0.21"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            try {
                val keystoreProperties = Properties()
                keystoreProperties.load(FileInputStream(file("../keystore.properties")))
                storeFile = file("bravebrain-release-key.keystore") // Direct reference to keystore in app directory
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            } catch (ex: Exception) {
                println("Keystore properties file not found, using placeholder values")
                storeFile = file("bravebrain-release-key.keystore")
                storePassword = "bravebrain2025"
                keyAlias = "bravebrain_key"
                keyPassword = "bravebrain2025"
            }
        }
    }
    
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    
    lint {
        abortOnError = false
        checkReleaseBuilds = false
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
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("androidx.viewpager2:viewpager2:1.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // Firebase BOM for version management
    implementation(platform("com.google.firebase:firebase-bom:34.3.0"))
    implementation("com.google.firebase:firebase-analytics")
    // Explicit versions for Firebase libraries that aren't resolving properly with BOM
    implementation("com.google.firebase:firebase-firestore-ktx:25.1.0")  
    implementation("com.google.firebase:firebase-auth-ktx:23.0.0")      
    implementation("com.google.firebase:firebase-storage-ktx:21.0.0")
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    implementation("com.google.android.gms:play-services-auth-api-phone:18.1.0")
    
    testImplementation(libs.junit)
    testImplementation("org.robolectric:robolectric:4.11.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
