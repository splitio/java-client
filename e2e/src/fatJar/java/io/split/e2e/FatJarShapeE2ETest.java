package io.split.e2e;

import io.split.client.SplitFactory;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Pins what the default artifact is: every bundled library present under its relocated name and none left under its
 * original one, where it would clash with the customer's own copy.
 */
public class FatJarShapeE2ETest {

    private static final String[] RELOCATED = {"split/com/google/", "split/org/apache/", "split/org/yaml/",
            "split/org/checkerframework/", "split/io/harness/events/"};
    private static final String[] MUST_NOT_LEAK = {"com/google/", "org/apache/", "org/yaml/", "org/checkerframework/",
            "io/harness/"};

    @Test
    public void bundlesEveryLibraryRelocatedAndLeaksNone() throws Exception {
        File jar = new File(SplitFactory.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        Assert.assertTrue("expected the fat jar on the classpath, got " + jar,
                jar.getName().startsWith("java-client-") && !jar.getName().contains("non-shaded"));

        boolean[] found = new boolean[RELOCATED.length];
        List<String> leaked = new ArrayList<>();
        try (JarFile jarFile = new JarFile(jar)) {
            for (Enumeration<JarEntry> entries = jarFile.entries(); entries.hasMoreElements(); ) {
                String name = entries.nextElement().getName();
                for (int i = 0; i < RELOCATED.length; i++) {
                    found[i] |= name.startsWith(RELOCATED[i]);
                }
                for (String prefix : MUST_NOT_LEAK) {
                    if (name.startsWith(prefix)) {
                        leaked.add(name);
                        break;
                    }
                }
            }
        }

        for (int i = 0; i < RELOCATED.length; i++) {
            Assert.assertTrue(RELOCATED[i] + " is missing from " + jar, found[i]);
        }
        Assert.assertTrue("unrelocated classes in " + jar + " (" + leaked.size() + " entries, e.g. "
                + (leaked.isEmpty() ? "" : leaked.get(0)) + ")", leaked.isEmpty());
    }
}
