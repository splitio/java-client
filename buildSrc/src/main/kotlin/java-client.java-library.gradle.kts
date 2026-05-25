plugins {
    `java-library`
    id("fme.publishing")
    id("fme.jacoco")
    id("fme.spotless")
    id("fme.sonarqube")
}

fmeSpotless {
    checkstyleConfigFile.set(rootProject.file(".github/linter/google-java-style.xml"))
    enforceSpotlessCheck.set(false)
}

java {
    withJavadocJar()
    withSourcesJar()
}

tasks.withType<Jar> {
    manifest {
        attributes(
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version
        )
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<Javadoc> {
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
}

// Ensure consistent JVM environment attribute resolution for all modules.
// Required by libraries like Guava (-jre variant) that use Gradle's
// variant-aware dependency resolution.
sourceSets.all {
    configurations.getByName(runtimeClasspathConfigurationName) {
        attributes.attribute(
            org.gradle.api.attributes.java.TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE,
            objects.named<org.gradle.api.attributes.java.TargetJvmEnvironment>(org.gradle.api.attributes.java.TargetJvmEnvironment.STANDARD_JVM)
        )
    }
    configurations.getByName(compileClasspathConfigurationName) {
        attributes.attribute(
            org.gradle.api.attributes.java.TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE,
            objects.named<org.gradle.api.attributes.java.TargetJvmEnvironment>(org.gradle.api.attributes.java.TargetJvmEnvironment.STANDARD_JVM)
        )
    }
}
