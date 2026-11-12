plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "fr.lpoirier.radios"
    compileSdk = 37

    defaultConfig {
        applicationId = "fr.lpoirier.radios"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

/**
 * Copie `config/radios.json` (source de vérité, à la racine du dépôt) dans les
 * assets générés de l'app : aucune duplication manuelle du fichier.
 */
abstract class CopyRadioConfigTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val configFile: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun copy() {
        val out = outputDir.get().asFile
        out.deleteRecursively()
        out.mkdirs()
        configFile.get().asFile.copyTo(out.resolve("radios.json"), overwrite = true)
    }
}

val copyRadioConfig = tasks.register<CopyRadioConfigTask>("copyRadioConfig") {
    configFile.set(rootProject.layout.projectDirectory.file("config/radios.json"))
    outputDir.set(layout.buildDirectory.dir("generated/radioConfig"))
}

androidComponents {
    onVariants { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(copyRadioConfig, CopyRadioConfigTask::outputDir)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.datastore.preferences)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)
    // Flux HLS (.m3u8), ex. RNE Radio 3 / Radio Clásica : détectés automatiquement par ExoPlayer.
    implementation(libs.media3.exoplayer.hls)

    implementation(libs.coil.compose)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.serialization.json)

    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
}

// Les tests unitaires lisent directement config/radios.json (source de vérité).
tasks.withType<Test>().configureEach {
    systemProperty("radios.config", rootProject.layout.projectDirectory.file("config/radios.json").asFile.absolutePath)
}
