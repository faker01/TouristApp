// Top-level build file where you can add configuration options common to all sub-projects/modules.

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

val mapkitApiKey: String by extra {
    val properties = java.util.Properties()
    project.file("local.properties").inputStream().use { properties.load(it) }
    properties.getProperty("MAPKIT_API_KEY", "")
}

