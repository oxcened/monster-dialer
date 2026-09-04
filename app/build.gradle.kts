import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
    alias(libs.plugins.hilt)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.firebase.crashlytics) apply false
}

val radiantEncounterDebugProperties = Properties().apply {
    val file = file("radiant-encounter-debug.properties")
    if (file.isFile) file.inputStream().use { load(it) }
}
val forceRadiantEncounters = radiantEncounterDebugProperties
    .getProperty("forceRadiantEncounters")
    .equals("true", ignoreCase = true)
val profileSharingHost = "monsterdialer.web.app"

// Firebase is used for production telemetry, but local debug builds should not
// require credentials that are intentionally excluded from source control.
if (file("google-services.json").isFile) {
    apply(plugin = libs.plugins.google.services.get().pluginId)
    apply(plugin = libs.plugins.firebase.crashlytics.get().pluginId)
}

val appVersionName = providers.gradleProperty("appVersionName").orNull
    ?: error("appVersionName must be set in gradle.properties")
val semanticVersion = Regex("""(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)""")
    .matchEntire(appVersionName)
    ?: error("appVersionName must use MAJOR.MINOR.PATCH semantic versioning")
val appVersionCode = semanticVersion.groupValues.drop(1).map(String::toLong).let { (major, minor, patch) ->
    require(minor <= 999 && patch <= 999) {
        "appVersionName minor and patch values must be at most 999"
    }
    val code = major * 1_000_000 + minor * 1_000 + patch
    require(code in 1..Int.MAX_VALUE.toLong()) {
        "appVersionName is too large to derive an Android versionCode"
    }
    code.toInt()
}

android {
    namespace = "dev.alenajam.monsterdialer"
    compileSdk = 37

    defaultConfig {
        applicationId = "dev.alenajam.monsterdialer"
        minSdk = 24
        targetSdk = 37
        versionCode = appVersionCode
        versionName = appVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["profileSharingHost"] = profileSharingHost
        buildConfigField("String", "PROFILE_SHARING_HOST", "\"$profileSharingHost\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
        isCoreLibraryDesugaringEnabled = true
    }

    val keystorePath = providers.environmentVariable("ANDROID_KEYSTORE_PATH").orNull
    buildTypes {
        debug {
            buildConfigField("boolean", "FORCE_RADIANT_ENCOUNTERS", forceRadiantEncounters.toString())
        }
        release {
            isMinifyEnabled = true
            buildConfigField("boolean", "FORCE_RADIANT_ENCOUNTERS", "false")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    if (keystorePath != null) {
        signingConfigs {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = providers.environmentVariable("ANDROID_KEYSTORE_PASSWORD").orNull
                    ?: error("ANDROID_KEYSTORE_PASSWORD must be set when signing a release")
                keyAlias = providers.environmentVariable("ANDROID_KEY_ALIAS").orNull
                    ?: error("ANDROID_KEY_ALIAS must be set when signing a release")
                keyPassword = providers.environmentVariable("ANDROID_KEY_PASSWORD").orNull
                    ?: error("ANDROID_KEY_PASSWORD must be set when signing a release")
            }
        }
        buildTypes.named("release") {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}

kotlin {
    jvmToolchain(21)
}

tasks.register("printReleaseVersion") {
    group = "release"
    description = "Prints the semantic version used for release validation."
    doLast {
        println(appVersionName)
    }
}

tasks.register("validateReleaseVersion") {
    group = "release"
    description = "Validates that RELEASE_TAG matches appVersionName."
    doLast {
        val releaseTag = providers.environmentVariable("RELEASE_TAG").orNull
            ?: error("RELEASE_TAG must be set, for example RELEASE_TAG=v$appVersionName")
        require(releaseTag == "v$appVersionName") {
            "Release tag $releaseTag does not match appVersionName $appVersionName"
        }
    }
}

dependencies {
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)
    implementation(monster.firebase.auth)
    implementation(monster.firebase.firestore)
    implementation(monster.firebase.storage)
    implementation(monster.coroutines.play.services)
    implementation(monster.androidx.credentials)
    implementation(monster.androidx.credentials.play.services.auth)
    implementation(monster.googleid)
    implementation(monster.libphonenumber)
    implementation(monster.zxing.core)

    // These would be Maven dependencies if OpenDialer were published
    // For now, we assume they are included in the settings.gradle.kts
    implementation(project(":core:common"))
    implementation(project(":feature:appShell"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:calls"))
    implementation(project(":feature:contacts"))
    implementation(project(":feature:inCall"))
    implementation(project(":data:contacts"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.runtime.livedata)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization)
    implementation(libs.coil.compose)
    implementation(libs.compose.activity)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation("sh.calvin.reorderable:reorderable:3.1.0")
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    kapt(libs.hilt.compiler)
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(monster.firebase.appcheck.debug)
    releaseImplementation(monster.firebase.appcheck.playintegrity)
}
