plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val syncDocsWebApp by tasks.registering(Sync::class) {
    group = "distribution"
    description = "Synchronisiert die komplette Web-App aus app/src/main/assets nach docs/ fuer GitHub Pages und installierte PWAs."

    from(layout.projectDirectory.dir("src/main/assets"))
    into(rootProject.layout.projectDirectory.dir("docs"))
    preserve {
        include(".nojekyll")
        include("README-GitHub-Pages.txt")
    }
}

android {
    namespace = "de.parip69.barcodeaudiscanner"
    compileSdk = 35

    defaultConfig {
        applicationId = "de.parip69.barcodeaudiscanner"
        minSdk = 24
        targetSdk = 35
        versionCode = 56
        versionName = "56"

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
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
    }

    applicationVariants.all {
        outputs.all {
            val output = this as com.android.build.gradle.internal.api.ApkVariantOutputImpl
            output.outputFileName = "BarcodeAudiScanner_ver${versionName}.apk"
        }
    }
}

tasks.named("preBuild") {
    dependsOn(syncDocsWebApp)
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.activity:activity-ktx:1.10.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
}
