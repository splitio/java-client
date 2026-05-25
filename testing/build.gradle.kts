plugins {
    id("java-client.java17")
}

group = "io.split.client"
version = "4.18.3"
description = "Testing suite for Java SDK for Split"

dependencies {
    api(project(":client"))
    compileOnly(libs.junit4)

    testImplementation(libs.junit4)
    testRuntimeOnly(libs.junit.vintage.engine)
    testImplementation("org.hamcrest:hamcrest-all:1.3")
}

// Override the default artifactId to match the Maven artifact name
publishing {
    publications.withType<MavenPublication>().configureEach {
        artifactId = "java-client-testing"
    }
}
