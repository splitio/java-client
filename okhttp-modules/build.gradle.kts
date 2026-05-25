plugins {
    id("java-client.java17")
}

group = "io.split.client"
version = "4.18.3"
description = "Alternative Http Modules"

dependencies {
    implementation(project(":client"))
    implementation(libs.okhttp)
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation(libs.httpclient5)

    testImplementation(libs.junit4)
    testRuntimeOnly(libs.junit.vintage.engine)
    testImplementation("org.mockito:mockito-core:1.10.19") // pinned to 1.x for legacy tests
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.gson) // needed for Json class used in tests
    testImplementation("org.hamcrest:hamcrest-all:1.3")

    // Powermock (legacy, needed for existing tests)
    testImplementation("org.powermock:powermock-module-junit4:1.7.4")
    testImplementation("org.powermock:powermock-api-mockito:1.7.4")
}
