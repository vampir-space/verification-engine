rootProject.name = "verification-engine"

pluginManagement {
    repositories {
        maven {
            name = "refinery-snapshots"
            url = uri("https://refinery.tools/maven/snapshots/")
        }
        gradlePluginPortal()
    }
}

plugins {
    id("tools.refinery.settings") version "0.3.1-SNAPSHOT"
}

includeBuild("./refinery-map/mapConvertertToRefinery/mapConverterToRefinery")
