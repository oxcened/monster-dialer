pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "MonsterDialer"

include(":app")

// Map OpenDialer modules to the other repo
val opendialerDir = "../opendialer"

// Gradle creates parent projects for nested paths. Point those parents at the
// OpenDialer repository too, otherwise Gradle 9 looks for ./core, ./data, and
// ./feature in this repository and rejects the missing directories.
listOf("core", "data", "feature").forEach { group ->
    include(":$group")
    project(":$group").projectDir = file("$opendialerDir/$group")
}

fun includeOpenDialer(path: String) {
    include(path)
    project(path).projectDir = file("$opendialerDir/${path.replace(":", "/")}")
}

includeOpenDialer(":core:common")
includeOpenDialer(":core:aosp")
includeOpenDialer(":data:calls")
includeOpenDialer(":data:callsCache")
includeOpenDialer(":data:contacts")
includeOpenDialer(":data:contactsSearch")
includeOpenDialer(":data:voicemail")
includeOpenDialer(":feature:callDetail")
includeOpenDialer(":feature:appShell")
includeOpenDialer(":feature:calls")
includeOpenDialer(":feature:contacts")
includeOpenDialer(":feature:contactsSearch")
includeOpenDialer(":feature:inCall")
includeOpenDialer(":feature:settings")
includeOpenDialer(":feature:voicemail")
