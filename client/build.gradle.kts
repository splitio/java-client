plugins {
    id("java-client.java17")
    alias(libs.plugins.shadow)
}

group = "io.split.client"
version = "4.18.3"
description = "Java SDK for Split"

dependencies {
    api(project(":pluggable-storage"))
    implementation(libs.guava)
    implementation(libs.slf4j.api)
    implementation(libs.httpclient5)
    implementation(libs.gson)
    implementation(libs.snakeyaml)

    testImplementation(libs.junit4)
    testRuntimeOnly(libs.junit.vintage.engine)
    testImplementation("org.hamcrest:hamcrest-all:1.3")
    testImplementation("org.mockito:mockito-core:1.10.19") // pinned to 1.x for legacy tests using MockitoJUnitRunner
    testImplementation(libs.slf4j.log4j12)
    testImplementation(libs.commons.lang3)
    testImplementation(libs.awaitility)
    testImplementation(libs.okhttp.mockwebserver)

    // Jersey test dependencies
    testImplementation("org.glassfish.jersey.media:jersey-media-sse:2.31")
    testImplementation("org.glassfish.jersey.inject:jersey-hk2:2.31")
    testImplementation("org.glassfish.jersey.containers:jersey-container-grizzly2-http:2.26")

    // Powermock (legacy, needed for existing tests)
    testImplementation("org.powermock:powermock-module-junit4:1.7.4")
    testImplementation("org.powermock:powermock-api-mockito:1.7.4")
}

// Resource filtering for splitversion.properties
tasks.processResources {
    filesMatching("splitversion.properties") {
        expand("project" to mapOf("version" to project.version))
    }
}

// Shaded fat JAR — include transitive deps, relocate to avoid conflicts
tasks.shadowJar {
    mergeServiceFiles()

    // Include only specific artifacts in the shadow jar
    dependencies {
        include(dependency(project(":pluggable-storage")))
        include(dependency("com.google.guava:guava"))
        include(dependency("com.google.code.gson:gson"))
        include(dependency("org.yaml:snakeyaml"))
        include(dependency("org.apache.httpcomponents.client5:httpclient5"))
        include(dependency("org.apache.httpcomponents.core5:httpcore5"))
        include(dependency("org.apache.httpcomponents.core5:httpcore5-h2"))
        include(dependency("org.checkerframework:checker-qual"))
        include(dependency("commons-codec:commons-codec"))
        include(dependency("com.google.errorprone:error_prone_annotations"))
        include(dependency("com.google.guava:failureaccess"))
        include(dependency("com.google.guava:listenablefuture"))
        include(dependency("com.google.j2objc:j2objc-annotations"))
    }

    relocate("org.apache", "split.org.apache")
    relocate("org.checkerframework", "split.org.checkerframework")
    relocate("org.yaml.snakeyaml", "split.org.yaml.snakeyaml")
    relocate("com.google", "split.com.google")

    exclude("META-INF/**", "LICENSE", "NOTICE", "/*.txt", "build.properties")
}
