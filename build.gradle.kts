plugins {
    java
    application
}

group = "space.vampir.engine"
version = "1.0-SNAPSHOT"

dependencies {
    implementation(refinery.generator)
    implementation("org.example:mapConverterToRefinery:1.0-SNAPSHOT")
    //implementation("org.eclipse.jetty.websocket:jetty-websocket-jetty-client")
    implementation("com.github.weisj:jsvg:latest.integration")
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
}

tasks.register("generateMapIndex") {
    val resourcesDir = sourceSets.main.get().resources.srcDirs.first()
    val outputDir = layout.buildDirectory.dir("generated/resources")

    inputs.dir(resourcesDir)
    outputs.dir(outputDir)

    doLast {
        val jsonFiles = fileTree(resourcesDir)
            .matching { include("**/*.json") }
            .map { "/${resourcesDir.toURI().relativize(it.toURI())}" }

        val outputFile = outputDir.get().file("map-list.txt").asFile
        outputFile.parentFile.mkdirs()
        outputFile.writeText(jsonFiles.joinToString("\n"))
    }
}

sourceSets.main.get().resources.srcDir(layout.buildDirectory.dir("generated/resources"))
tasks.processResources.get().dependsOn("generateMapIndex")