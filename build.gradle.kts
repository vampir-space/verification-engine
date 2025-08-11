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
