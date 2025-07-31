plugins {
    id("java")
}

group = "space.vampir.engine"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}
//
dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
    implementation(refinery.generator)
}
//
tasks.getByName<Test>("test") {
    useJUnitPlatform()
}