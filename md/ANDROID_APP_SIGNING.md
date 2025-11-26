# Android App Signing Guide for BraveBrain App

This document outlines the process of generating a keystore, configuring signing settings, and building a signed release APK for the BraveBrain Android application.

## Prerequisites

- Java Development Kit (JDK) installed with keytool available
- Android Studio or command-line tools
- Gradle build system

## Step 1: Generate Keystore File

Use the keytool command to generate a new keystore file:

```bash
keytool -genkey -v -keystore app/bravebrain-release-key.keystore -alias bravebrain_key -keyalg RSA -keysize 2048 -validity 10000 -storepass bravebrain2025 -keypass bravebrain2025 -dname "CN=BraveBrain,OU=Development,O=BraveBrainApp,L=Bengaluru,S=Karnataka,C=IN"
```

This command creates a keystore file with:
- 2048-bit RSA key algorithm
- 10,000-day validity period
- Specified passwords for store and key
- Distinguished name with organizational details

## Step 2: Configure Signing in build.gradle.kts

The app/build.gradle.kts file has been updated with signing configuration:

```kotlin
import java.util.Properties
import java.io.FileInputStream

android {
    // ... other configurations
    
    signingConfigs {
        create("release") {
            try {
                val keystoreProperties = Properties()
                keystoreProperties.load(FileInputStream(file("../keystore.properties")))
                storeFile = file("bravebrain-release-key.keystore")
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
}
```

## Step 3: Create keystore.properties File

A keystore.properties file has been created in the project root:

```properties
# Keystore configuration for BraveBrain app
storeFile=bravebrain-release-key.keystore
storePassword=bravebrain2025
keyAlias=bravebrain_key
keyPassword=bravebrain2025
```

## Step 4: Build Signed Release APK

To build a signed release APK, run:

```bash
./gradlew assembleRelease
```

The signed APK will be generated at:
`app/build/outputs/apk/release/app-release.apk`

## Security Considerations

- Keep the keystore file secure and backed up
- Never commit the keystore file to version control
- Protect the keystore password and key password
- Consider using environment variables for sensitive information in production environments

## Troubleshooting

### Keystore not found error
- Ensure the keystore file exists in the correct location
- Verify the path in build.gradle.kts is correct

### Build fails with signing error
- Check that the keystore password and key password are correct
- Verify the key alias matches what was used during keystore generation

## Notes

The generated APK is properly signed and ready for distribution through app stores or direct download. The signing configuration ensures that all release builds will be automatically signed with the same key, which is required for app updates.