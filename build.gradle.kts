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
            .matching {
                include("**/*.json")
                exclude("RunConfigurations/**")
            }
            .map { "/${resourcesDir.toURI().relativize(it.toURI())}" }

        val outputFile = outputDir.get().file("map-list.txt").asFile
        outputFile.parentFile.mkdirs()
        outputFile.writeText(jsonFiles.joinToString("\n"))
    }
}

sourceSets.main.get().resources.srcDir(layout.buildDirectory.dir("generated/resources"))
tasks.processResources.get().dependsOn("generateMapIndex")

// Helper to parse command-line args passed via -PappArgs="--map /path --metamodel m"
fun parseAppArgs(): List<String> {
    val raw = if (project.hasProperty("appArgs")) project.property("appArgs")?.toString() else null
    return raw?.split(" ")?.filter { it.isNotBlank() } ?: listOf()
}

// JavaExec tasks for main classes.

// ROSReplayer has multiple nested static classes each with a main method. Use $ in class name (escaped).
tasks.register("visualize", JavaExec::class) {
    group = "application"
    description = "Run ROSReplayer.NoVerificationEngineRealConfiguration main to visualize without Refinery"
    mainClass.set("space.vampir.engine.ROSReplayer\$NoVerificationEngineRealConfiguration")
    classpath = sourceSets.main.get().runtimeClasspath
    args = parseAppArgs()
}

tasks.register("experiment", JavaExec::class) {
    group = "application"
    description = "Run ROSReplayer.RefineryVerificationEngineRealConfiguration main to run Refinery with the real vehicle"
    mainClass.set("space.vampir.engine.ROSReplayer\$RefineryVerificationEngineRealConfiguration")
    classpath = sourceSets.main.get().runtimeClasspath
    args = parseAppArgs()
}

tasks.register("experiment-sim", JavaExec::class) {
    group = "application"
    description = "Run ROSReplayer.RefineryVerificationEngineRunConfiguration main to run Refinery with the real vehicle"
    mainClass.set("space.vampir.engine.ROSReplayer\$RefineryVerificationEngineRunConfiguration")
    classpath = sourceSets.main.get().runtimeClasspath
    args = parseAppArgs()
}

tasks.register("experiment-yolo", JavaExec::class) {
    group = "application"
    description = "Run ROSReplayer.YoloErrorCalculation main"
    mainClass.set("space.vampir.engine.ROSReplayer\$YoloErrorCalculation")
    classpath = sourceSets.main.get().runtimeClasspath
    args = parseAppArgs()
}
