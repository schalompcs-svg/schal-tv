plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.schal.tv"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.schal.tv"
        // minSdk 21 : compromis choisi pour couvrir des téléphones Android
        // anciens/modestes tout en gardant accès aux API MediaStore modernes
        // nécessaires au scan hors-ligne (voir docs/ARCHITECTURE.md).
        minSdk = 21
        targetSdk = 34
        versionCode = 4
        versionName = "0.4"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = true
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

    testOptions {
        unitTests {
            isReturnDefaultValues = true
            isIncludeAndroidResources = false
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")

    // Lecteur vidéo : androidx.media3 (successeur officiel d'ExoPlayer,
    // maintenu par Google). Choisi car : support natif HLS (.m3u8) et MP4,
    // gestion intégrée des états d'erreur réseau/format, empreinte mémoire
    // raisonnable, et c'est la bibliothèque multimédia recommandée à ce jour
    // pour Android (l'ancien dépôt "google/ExoPlayer" est arrêté et redirige
    // vers media3). Pas d'alternative plus légère offrant HLS + DASH + mp4
    // avec autant de fiabilité sans écrire notre propre démultiplexeur HLS.
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")

    testImplementation("junit:junit:4.13.2")
    // org.json réel pour les tests JVM (le org.json du android.jar de test est
    // un stub qui lève une exception à l'exécution) : nécessaire uniquement
    // en testImplementation, jamais embarqué dans l'APK.
    testImplementation("org.json:json:20240303")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
}
