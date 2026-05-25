rootProject.name = "java-client"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    versionCatalogs {
        create("libs") {
            from("io.harness.fme:fme-version-catalog:0.6.0")
        }
    }
    repositories {
        mavenLocal {
            content {
                includeGroupByRegex("io\\.split\\..*")
                includeGroupByRegex("io\\.harness\\.fme.*")
            }
        }
        maven {
            name = "HarnessMaven"
            url = uri("https://harness0.harness.io/pkg/l7B_kbSEQD2wjrM7PShm5w/fme-mvn/maven")
            content {
                includeGroupByRegex("io\\.split\\..*")
                includeGroupByRegex("io\\.harness\\.fme.*")
            }
        }
        maven {
            name = "CodeArtifact"
            url = uri("https://fme-079419646996.d.codeartifact.us-east-1.amazonaws.com/maven/maven-fme/")
            credentials {
                username = "aws"
                password = providers.environmentVariable("CODEARTIFACT_AUTH_TOKEN")
                    .orElse(providers.gradleProperty("codeArtifactToken"))
                    .getOrElse("")
            }
            content {
                includeGroupByRegex("io\\.split\\..*")
                includeGroupByRegex("io\\.harness\\.fme.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

include(
    "pluggable-storage",
    "redis-wrapper",
    "testing",
    "okhttp-modules",
    "client"
)
