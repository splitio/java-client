package io.split.e2e;

import io.split.client.testing.SplitClientForTest;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Checks the released java-client-testing jar (pinned in build.gradle) against the java-client built from this tree.
 * Every java-client interface method that a testing class implements must have a concrete implementation in that class.
 * A method added to a java-client interface after the testing release is not implemented by it, and nothing fails at
 * compile or load time: the call throws AbstractMethodError later. This test is the only place that surfaces it.
 */
public class ReleasedTestingCompatE2ETest {

    private static final String TESTING_PACKAGE = "io/split/client/testing/";
    private static final String TESTING_CLASS_PACKAGE = "io.split.client.testing.";

    @Test
    public void releasedTestingImplementsEveryJavaClientInterfaceMethod() throws Exception {
        String testingVersion = System.getProperty("compat.testing.version");
        String sdkVersion = System.getProperty("compat.sdk.version");
        Assert.assertNotNull("compat.testing.version system property missing (set in e2e/build.gradle)", testingVersion);
        Assert.assertNotNull("compat.sdk.version system property missing (set in e2e/build.gradle)", sdkVersion);

        File jar = new File(SplitClientForTest.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        Assert.assertEquals("expected the java-client-testing " + testingVersion + " jar on the test classpath, found " + jar.getName(),
                "java-client-testing-" + testingVersion + ".jar", jar.getName());

        List<String> missing = new ArrayList<>();
        try (JarFile jarFile = new JarFile(jar)) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                String entry = entries.nextElement().getName();
                if (!entry.startsWith(TESTING_PACKAGE) || !entry.endsWith(".class")) {
                    continue;
                }
                String className = entry.substring(0, entry.length() - ".class".length()).replace('/', '.');
                Class<?> type = Class.forName(className, false, getClass().getClassLoader());
                if (type.isInterface() || Modifier.isAbstract(type.getModifiers())) {
                    continue;
                }
                for (Class<?> sdkInterface : sdkInterfaces(type)) {
                    for (Method method : sdkInterface.getDeclaredMethods()) {
                        if (Modifier.isAbstract(method.getModifiers()) && !isImplemented(type, method)) {
                            missing.add("  - " + type.getName() + " does not implement " + sdkInterface.getName() + "#"
                                    + method.getName() + describeParameters(method));
                        }
                    }
                }
            }
        }

        Assert.assertTrue("java-client-testing " + testingVersion + " does not implement methods that java-client "
                + sdkVersion + " requires:\n" + String.join("\n", missing) + "\n\n"
                + "Calling any of them throws AbstractMethodError at runtime. Nothing fails at compile or load time, so this test is the only check.\n"
                + "Fix one of two ways:\n"
                + "  1. Keep the java-client change compatible with java-client-testing " + testingVersion + ".\n"
                + "  2. Release a java-client-testing built against java-client " + sdkVersion + ", then set compatTestingVersion in "
                + "e2e/build.gradle to it. " + testingVersion + " then stops being supported with java-client " + sdkVersion + ".\n"
                + "Background: e2e/README.md, section \"Released testing compatibility\".",
                missing.isEmpty());
    }

    /**
     * Every java-client interface the class implements, directly or through a superinterface or superclass.
     * Interfaces from the testing jar itself are skipped: they are released with it, not with the SDK.
     */
    private static Set<Class<?>> sdkInterfaces(Class<?> type) {
        Set<Class<?>> found = new LinkedHashSet<>();
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            collectInterfaces(current, found);
        }
        found.removeIf(iface -> iface.getName().startsWith(TESTING_CLASS_PACKAGE));
        return found;
    }

    private static void collectInterfaces(Class<?> type, Set<Class<?>> found) {
        for (Class<?> iface : type.getInterfaces()) {
            if (found.add(iface)) {
                collectInterfaces(iface, found);
            }
        }
    }

    /**
     * Class.getMethod returns the abstract interface method when nothing in the class implements it, so an abstract
     * result means the method is missing.
     */
    private static boolean isImplemented(Class<?> type, Method interfaceMethod) {
        try {
            Method resolved = type.getMethod(interfaceMethod.getName(), interfaceMethod.getParameterTypes());
            return !Modifier.isAbstract(resolved.getModifiers());
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    private static String describeParameters(Method method) {
        StringBuilder description = new StringBuilder("(");
        Class<?>[] parameters = method.getParameterTypes();
        for (int i = 0; i < parameters.length; i++) {
            if (i > 0) {
                description.append(", ");
            }
            description.append(parameters[i].getSimpleName());
        }
        return description.append(")").toString();
    }
}
