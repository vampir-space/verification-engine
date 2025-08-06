plugins {
    java
    application
}

group = "space.vampir.engine"
version = "1.0-SNAPSHOT"

dependencies {
    implementation(refinery.generator)
    implementation("org.example:mapConverterToRefinery:1.0-SNAPSHOT")
}
