plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "ict.mgame.iotmedicinebox"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "ict.mgame.iotmedicinebox"
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
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.cardview)
    implementation(libs.kommunicateui)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.legacy.support.v4)
    implementation(libs.material.v1110)

}

configurations.all {
    exclude(group = "com.android.support")
    exclude(group = "android.support")
}