plugins {
    `kotlin-dsl`
}

repositories {
    mavenLocal()
    maven {
        url = uri("https://harness0.harness.io/pkg/l7B_kbSEQD2wjrM7PShm5w/fme-mvn/maven")
    }
    maven {
        url = uri("https://fme-079419646996.d.codeartifact.us-east-1.amazonaws.com/maven/maven-fme/")
        credentials {
            username = "aws"
            password = System.getenv("CODEARTIFACT_AUTH_TOKEN") ?: ""
        }
    }
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation("io.harness.fme:fme-gradle-plugin:0.5.0")
}
