plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.devtools.ksp)
}

android {
    namespace = "com.app.medbox_wifi"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.app.medbox_wifi"
        minSdk = 30
        targetSdk = 35
        versionCode = 3
        versionName = "1.1.2"

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

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// Robust task to clean all launcher icons and setup the new one correctly
tasks.register("setupLauncherIcon") {
    doLast {
        val resDir = file("src/main/res")
        val logoFile = file("src/main/assets/logo/medicine_bin.png")
        
        if (!logoFile.exists()) {
            throw GradleException("Logo file not found at ${logoFile.absolutePath}")
        }

        // 1. Delete ALL existing ic_launcher files everywhere in mipmap folders
        resDir.listFiles { f -> f.isDirectory && f.name.startsWith("mipmap") }?.forEach { dir ->
            dir.listFiles { f -> f.name.startsWith("ic_launcher") }?.forEach { it.delete() }
        }

        // 2. Setup the drawable folders
        val xxxhdpiDir = File(resDir, "drawable-xxxhdpi")
        if (!xxxhdpiDir.exists()) xxxhdpiDir.mkdirs()
        logoFile.copyTo(File(xxxhdpiDir, "ic_app_logo.png"), true)

        // 3. Create/Update Adaptive Icon XMLs in anydpi
        val anyDpiDir = File(resDir, "mipmap-anydpi")
        if (!anyDpiDir.exists()) anyDpiDir.mkdirs()

        File(anyDpiDir, "ic_launcher.xml").writeText("""
            <?xml version="1.0" encoding="utf-8"?>
            <adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
                <background android:drawable="@drawable/ic_launcher_background" />
                <foreground android:drawable="@drawable/ic_launcher_foreground" />
            </adaptive-icon>
        """.trimIndent())

        File(anyDpiDir, "ic_launcher_round.xml").writeText("""
            <?xml version="1.0" encoding="utf-8"?>
            <adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
                <background android:drawable="@drawable/ic_launcher_background" />
                <foreground android:drawable="@drawable/ic_launcher_foreground" />
            </adaptive-icon>
        """.trimIndent())
        
        println("Launcher icons successfully reset and setup as Adaptive Icons.")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // ML Kit & CameraX
    implementation(libs.mlkit.text.recognition)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // JSON Serialization
    implementation("com.google.code.gson:gson:2.10.1")
    
    // HTTP Client (Volley or OkHttp) - Using Volley for simplicity in this example
    implementation("com.android.volley:volley:1.2.1")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}