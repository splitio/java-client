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

## Released testing compatibility

`ReleasedTestingCompatE2ETest` checks the published `io.split.client:java-client-testing` release (pinned as
`compatTestingVersion` in `build.gradle`, resolved from Maven Central only) against the SDK built from this tree. It fails
when that release implements a `java-client` interface without the methods the SDK now requires, which would throw
`AbstractMethodError` at runtime for consumers. The failure message lists each missing method and the two ways to fix it:

1. Keep the SDK change compatible with the pinned testing release.
2. Release a new `java-client-testing` built against this SDK, then set `compatTestingVersion` to it. The old version
   is then unsupported with this SDK.

The check only covers methods the released jar implements; behavioural changes need their own tests. To check a local
testing build without publishing it, run `./gradlew test -Pcompat.testing.version=X` after installing it to mavenLocal;
the override also lifts the Maven Central restriction, so mavenLocal is consulted. Without it, the default version
is resolved from Maven Central only.
