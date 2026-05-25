plugins {
    id("java-client.java17")
    alias(libs.plugins.shadow)
}

group = "io.split.client"
version = "3.1.2"
description = "Implements Redis Pluggable Storage"

dependencies {
    api(project(":pluggable-storage"))
    implementation("redis.clients:jedis:4.4.8")

    testImplementation(libs.junit4)
    testRuntimeOnly(libs.junit.vintage.engine)
}

tasks.shadowJar {
    mergeServiceFiles()
    // jedis is intentionally NOT relocated — consumers depend on redis.clients.jedis types directly
    exclude(
        "META-INF/license/**",
        "META-INF/*",
        "META-INF/maven/**",
        "META-INF/services/**",
        "LICENSE",
        "NOTICE",
        "/*.txt",
        "build.properties"
    )
}
