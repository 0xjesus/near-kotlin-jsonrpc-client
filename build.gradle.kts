plugins {
    kotlin("jvm") version "1.9.22" apply false
    kotlin("plugin.serialization") version "1.9.22" apply false
    id("org.jetbrains.kotlinx.kover") version "0.7.5"
    id("maven-publish")
}

allprojects {
    group = "org.near"
    version = "0.1.0"
    
    repositories {
        mavenCentral()
    }
}

koverReport {
    defaults {
        filters {
            excludes {
                packages("org.openapitools.*")
            }
        }
        verify {
            rule {
                minBound(80)
            }
        }
    }
}
