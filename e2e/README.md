# e2e

Mimics a real consumer of java-client. It builds the SDK, installs it to mavenLocal, then consumes it from there
in a separate Gradle build: the `non-shaded` variant (`test`) and the fat jar (`fatJarTest`). Nothing in the
module provides `io.harness:events` or the other libraries the artifacts are meant to bundle.

Each variant has three tests: it initialises the SDK in localhost mode, checks the jar's contents (bundled and
relocated libraries), and runs `jdeps` to fail on any referenced class that can't be resolved.

```
cd e2e && ./gradlew test fatJarTest
```

Requires Maven and JDK 17 on the path. The SDK version is read from the parent `pom.xml`.
