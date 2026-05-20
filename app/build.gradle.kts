import com.android.build.gradle.internal.tasks.factory.dependsOn
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

private val readAndUnderstoodLicense = false

plugins {
    id("com.android.application")
    id("io.gitlab.arturbosch.detekt")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

android {
    namespace = "heitezy.peekdisplay"
    compileSdk = 36

    defaultConfig {
        applicationId = "heitezy.peekdisplay"
        minSdk = 24
        //noinspection OldTargetApi
        targetSdk = 35
        versionCode = 14
        versionName = "1.1.1"
    }

    buildFeatures {
        compose = true
    }

    buildTypes {
        debug {
            isMinifyEnabled = true
            isShrinkResources = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = " debug"
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    buildFeatures {
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    lint {
        disable += "MissingTranslation"
    }
    project.tasks.preBuild.dependsOn("license")
}

detekt {
    config.setFrom(file("detekt-config.yml"))
    buildUponDefaultConfig = true
    basePath = rootProject.projectDir.absolutePath
}

tasks.register("license") {
    doFirst {
        val data =
            file("./src/main/java/heitezy/peekdisplay/activities/AboutActivity.kt")
                .readText()
                .contains("title = stringResource(R.string.about_license)")
        if (!data) {
            throw Exception(
                "Please note that removing the license from the about page is not allowed if you " +
                    "plan to publish your modified version of this app. " +
                    "Please read the project's LICENSE.",
            )
        }
        if (!(
                android.defaultConfig.applicationId?.contains("heitezy") == true ||
                    readAndUnderstoodLicense
            )
        ) {
            throw Exception(
                "Please make sure you have read and understood the LICENSE!",
            )
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.04.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("com.google.android.material:material:1.13.0")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("com.android.volley:volley:1.2.1")
}
