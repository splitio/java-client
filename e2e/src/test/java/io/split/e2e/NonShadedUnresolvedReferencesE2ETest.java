package io.split.e2e;

import io.split.client.SplitFactory;
import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * The non-shaded jar leaves guava, httpclient5, snakeyaml and gson to the consumer, so they are resolved from the test classpath.
 * Runs jdeps over the jar and fails on any referenced class that cannot be resolved, so a package added to the
 * SDK but left out of the build's include list is caught even though initialising the SDK would still work.
 * The optional pluggable-storage module is the only expected gap.
 */
public class NonShadedUnresolvedReferencesE2ETest {

    @Test
    public void everyReferencedClassIsResolvable() throws Exception {
        File jar = new File(SplitFactory.class.getProtectionDomain().getCodeSource().getLocation().toURI());

        List<String> command = new ArrayList<>();
        command.add(new File(System.getProperty("java.home"), "bin/jdeps").getPath());
        command.add("--multi-release");
        command.add("base");
        command.add("-verbose:class");
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add(jar.getPath());
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();

        List<String> unresolved = new ArrayList<>();
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            for (String line; (line = reader.readLine()) != null; ) {
                output.append(line).append('\n');
                if (line.endsWith("not found") && !line.startsWith(jar.getName()) && !line.contains("-> pluggable.")) {
                    unresolved.add(line.trim());
                }
            }
        }
        Assert.assertEquals("jdeps failed:\n" + output, 0, process.waitFor());
        Assert.assertTrue("classes referenced but missing from the non-shaded jar and from the consumer dependencies, in " + jar + ": " + unresolved, unresolved.isEmpty());
    }
}
