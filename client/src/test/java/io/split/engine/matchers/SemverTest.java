package io.split.engine.matchers;

import org.junit.Test;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;

/**
 * Tests for AllKeysMatcher
 */
public class SemverTest {

    @Test
    public void testValidVersions() throws IOException {
        List<List<String>> versions = new ArrayList<>();
        BufferedReader br = new BufferedReader(new FileReader("src/test/resources/semver/valid-semantic-versions.csv"));
        String line;
        boolean firstLine = true;
        while ((line = br.readLine()) != null) {
            if (firstLine) {firstLine = false; continue; }
            String[] values = line.split(",");
            versions.add(Arrays.asList(values));
        }
        for(List<String> version : versions) {
            assertTrue(Semver.build(version.get(0)) != null);
            assertTrue(Semver.build(version.get(1)) != null);
        }
    }

    @Test
    public void testInvalidVersions() throws IOException {
        List<List<String>> versions = new ArrayList<>();
        BufferedReader br = new BufferedReader(new FileReader("src/test/resources/semver/invalid-semantic-versions.csv"));
        String line;
        boolean firstLine = true;
        while ((line = br.readLine()) != null) {
            if (firstLine) {firstLine = false; continue; }
            String[] values = line.split(",");
            versions.add(Arrays.asList(values));
        }
        for(List<String> version : versions) {
            assertTrue(Semver.build(version.get(0)) == null);
        }
    }

    @Test
    public void testCompareVersions() throws IOException {
        List<List<String>> versions = new ArrayList<>();
        BufferedReader br = new BufferedReader(new FileReader("src/test/resources/semver/valid-semantic-versions.csv"));
        String line;
        boolean firstLine = true;
        while ((line = br.readLine()) != null) {
            if (firstLine) {firstLine = false; continue; }
            String[] values = line.split(",");
            versions.add(Arrays.asList(values));
        }
        for(List<String> version : versions) {
            assertTrue(Semver.build(version.get(0)).compare(Semver.build(version.get(1))) == 1);
            assertTrue(Semver.build(version.get(1)).compare(Semver.build(version.get(0))) == -1);
        }

        versions.clear();
        br = new BufferedReader(new FileReader("src/test/resources/semver/equal-to-semver.csv"));
        firstLine = true;
        while ((line = br.readLine()) != null) {
            if (firstLine) {firstLine = false; continue; }
            String[] values = line.split(",");
            versions.add(Arrays.asList(values));
        }
        for(List<String> version : versions) {
            Semver version1 = Semver.build(version.get(0));
            Semver version2 = Semver.build(version.get(1));

            if (version.get(2).equals("true")) {
                assertTrue(version1.version().equals(version2.version()));
            } else {
                assertTrue(!version1.version().equals(version2.version()));
            }
        }

        versions.clear();
        br = new BufferedReader(new FileReader("src/test/resources/semver/between-semver.csv"));
        firstLine = true;
        while ((line = br.readLine()) != null) {
            if (firstLine) {firstLine = false; continue; }
            String[] values = line.split(",");
            versions.add(Arrays.asList(values));
        }
        for(List<String> version : versions) {
            Semver version1 = Semver.build(version.get(0));
            Semver version2 = Semver.build(version.get(1));
            Semver version3 = Semver.build(version.get(2));

            if (version.get(3).equals("true")) {
                assertTrue(version2.compare(version1) >= 0 && version3.compare(version2) >= 0);
            } else {
                assertTrue(version2.compare(version1) < 0 || version3.compare(version2) < 0);
            }
        }

    }
    @Test
    public void testLeadingZeros() {
        assertTrue(Semver.build("1.01.2").version().equals("1\\.1\\.2"));
        assertTrue(Semver.build("1.01.2-rc.01").version().equals("1\\.1\\.2-rc\\.1"));
    }
}
