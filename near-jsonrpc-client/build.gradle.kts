plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    jacoco
    `maven-publish`
    signing
}
java {
    toolchain { languageVersion.set(org.gradle.jvm.toolchain.JavaLanguageVersion.of(17)) }
}
repositories { mavenCentral() }
dependencies {
    implementation(project(":near-jsonrpc-types"))
    implementation("io.ktor:ktor-client-core:2.3.9")
    implementation("io.ktor:ktor-client-cio:2.3.9")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.9")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.9")
    testImplementation("io.ktor:ktor-client-mock:2.3.9")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
}
tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}
jacoco { toolVersion = "0.8.11" }
tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports { 
        xml.required.set(true)
        html.required.set(true) 
    }
    
    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) {
                // SOLO medir las clases públicas del API
                include("org/near/jsonrpc/client/NearJsonRpcClient.class")
                include("org/near/jsonrpc/client/NearJsonRpcClient\$*.class")
                include("org/near/jsonrpc/client/NearRpcException.class")
                
                // EXCLUIR todo lo demás (código generado y clases privadas)
                exclude("org/near/jsonrpc/client/NearJsonRpcClientGeneratedKt.class")
                exclude("org/near/jsonrpc/client/Rpc*.class") // Excluir RpcErr, RpcReq, RpcRes (privadas)
            }
        })
    )
}
tasks.register<Exec>("generateKotlinClient") {
    val outDir = layout.projectDirectory.dir("build/generated-src/org/near/jsonrpc/client")
    outputs.dir(outDir)
    workingDir = rootDir
    dependsOn(":near-jsonrpc-types:downloadOpenApiSpec")
    environment("SPEC_FILE", rootDir.resolve("near-jsonrpc-types/build/openapi.json").absolutePath)
    commandLine("python3", rootDir.resolve("tools/gen_client_from_openapi.py").absolutePath, outDir.asFile.absolutePath)
}

tasks.compileKotlin { dependsOn("generateKotlinClient") }
sourceSets { named("main") { kotlin.srcDir("build/generated-src") } }

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            groupId = (project.findProperty("GROUP") as String?) ?: "com.github.nearclient"
            artifactId = "near-jsonrpc-client"
            version = (project.findProperty("VERSION_NAME") as String?) ?: "0.1.0-SNAPSHOT"
            pom {
                name.set("NEAR JSON-RPC Kotlin Client")
                description.set("Developer-friendly Kotlin client for NEAR JSON-RPC")
                url.set((project.findProperty("POM_URL") as String?) ?: "https://github.com/placeholder/repo")
                licenses { license {
                    name.set((project.findProperty("POM_LICENSE_NAME") as String?) ?: "Apache-2.0")
                    url.set((project.findProperty("POM_LICENSE_URL") as String?) ?: "https://www.apache.org/licenses/LICENSE-2.0.txt")
                } }
                scm {
                    url.set((project.findProperty("POM_SCM_URL") as String?) ?: "https://github.com/placeholder/repo")
                    connection.set((project.findProperty("POM_SCM_CONNECTION") as String?) ?: "scm:git:git://github.com/placeholder/repo.git")
                    developerConnection.set((project.findProperty("POM_SCM_DEV_CONNECTION") as String?) ?: "scm:git:ssh://git@github.com/placeholder/repo.git")
                }
                developers { developer {
                    id.set((project.findProperty("POM_DEVELOPER_ID") as String?) ?: "dev")
                    name.set((project.findProperty("POM_DEVELOPER_NAME") as String?) ?: "Developer")
                } }
            }
        }
    }
    repositories {
        maven {
            name = "sonatype"
            val releasesRepoUrl = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            val snapshotsRepoUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
            credentials {
                username = findProperty("sonatypeUsername")?.toString() ?: System.getenv("ORG_GRADLE_PROJECT_sonatypeUsername")
                password = findProperty("sonatypePassword")?.toString() ?: System.getenv("ORG_GRADLE_PROJECT_sonatypePassword")
            }
        }
    }
}
signing {
    val signingKey: String? = findProperty("SIGNING_KEY") as String?
    val signingPassword: String? = findProperty("SIGNING_PASSWORD") as String?
    if (!signingKey.isNullOrBlank() && !signingPassword.isNullOrBlank()) {
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications)
    }
}
tasks.register<Exec>("postGenRaw") {
    group = "codegen"
    workingDir = rootProject.projectDir
    commandLine("python3", "scripts/generate_client_raw.py")
    isIgnoreExitValue = false
    inputs.files("scripts/generate_client_raw.py")
    outputs.upToDateWhen { false }
}
tasks.named("compileKotlin") {
    dependsOn("generateKotlinClient", "postGenRaw")
}
