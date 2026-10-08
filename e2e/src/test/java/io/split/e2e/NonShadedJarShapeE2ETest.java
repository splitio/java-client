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
 * Pins what the `non-shaded` artifact is: the thin jar plus the relocated io.harness:events, and nothing else
 * bundled. Initialising the SDK (NonShadedConsumerE2ETest) cannot tell this apart from the fat jar, which also
 * works, so a build that silently produces the fat jar under the non-shaded name would otherwise go unnoticed.
 */
public class NonShadedJarShapeE2ETest {

    private static final String[] BUNDLED_LIBRARIES = {"split/com/google/", "split/org/apache/", "split/org/yaml/",
            "split/org/checkerframework/"};

    @Test
    public void bundlesOnlyTheRelocatedEventsLibrary() throws Exception {
        File jar = new File(SplitFactory.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        Assert.assertTrue("expected the non-shaded jar on the classpath, got " + jar, jar.getName().endsWith("-non-shaded.jar"));

        boolean hasEventsConfig = false;
        List<String> unrelocatedEvents = new ArrayList<>();
        List<String> bundledLibraries = new ArrayList<>();
        try (JarFile jarFile = new JarFile(jar)) {
            for (Enumeration<JarEntry> entries = jarFile.entries(); entries.hasMoreElements(); ) {
                String name = entries.nextElement().getName();
                hasEventsConfig |= name.equals("split/io/harness/events/EventsManagerConfig.class");
                if (name.startsWith("io/harness/")) {
                    unrelocatedEvents.add(name);
                }
                for (String library : BUNDLED_LIBRARIES) {
                    if (name.startsWith(library)) {
                        bundledLibraries.add(name);
                        break;
                    }
                }
            }
        }

        Assert.assertTrue("relocated io.harness.events classes are missing from " + jar, hasEventsConfig);
        Assert.assertTrue("unrelocated io.harness classes in " + jar + ": " + unrelocatedEvents, unrelocatedEvents.isEmpty());
        Assert.assertTrue(jar + " bundles libraries that consumers of the non-shaded variant provide themselves (" + bundledLibraries.size()
                + " entries, e.g. " + (bundledLibraries.isEmpty() ? "" : bundledLibraries.get(0)) + ")", bundledLibraries.isEmpty());
    }
}
