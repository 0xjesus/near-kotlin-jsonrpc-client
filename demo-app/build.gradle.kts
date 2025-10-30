plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.near"
version = "1.0.0"

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    // Use the local library
    implementation(project(":near-jsonrpc-client"))
    implementation(project(":near-jsonrpc-types"))

    // Ktor server
    implementation("io.ktor:ktor-server-core:2.3.9")
    implementation("io.ktor:ktor-server-netty:2.3.9")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.9")
    implementation("io.ktor:ktor-server-cors:2.3.9")
    implementation("io.ktor:ktor-server-call-logging:2.3.9")
    implementation("io.ktor:ktor-server-status-pages:2.3.9")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.9")

    // Ktor client (needed for HttpClient type)
    implementation("io.ktor:ktor-client-core:2.3.9")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.4.14")
}

application {
    mainClass.set("com.near.demo.ApplicationKt")
}

tasks.shadowJar {
    archiveBaseName.set("near-demo")
    archiveClassifier.set("")
    archiveVersion.set("")
}
