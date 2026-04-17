package io.split.rules.matchers;

import org.junit.Test;

import static org.junit.Assert.*;

public class SemverTest {

    @Test
    public void buildsValidSemver() {
        Semver s = Semver.build("1.2.3");
        assertNotNull(s);
        assertEquals("1.2.3", s.version());
        assertEquals(Long.valueOf(1), s.major());
        assertEquals(Long.valueOf(2), s.minor());
        assertEquals(Long.valueOf(3), s.patch());
    }

    @Test
    public void buildsWithPreRelease() {
        Semver s = Semver.build("1.0.0-alpha.1");
        assertNotNull(s);
        assertFalse(s.isStable());
    }

    @Test
    public void returnsNullForEmpty() {
        assertNull(Semver.build(""));
    }

    @Test
    public void returnsNullForInvalidFormat() {
        assertNull(Semver.build("notasemver"));
    }

    @Test
    public void compareReturnsZeroForEqual() {
        assertEquals(0, Semver.build("1.2.3").compare(Semver.build("1.2.3")));
    }

    @Test
    public void compareOrdersCorrectly() {
        assertTrue(Semver.build("2.0.0").compare(Semver.build("1.0.0")) > 0);
        assertTrue(Semver.build("1.0.0").compare(Semver.build("2.0.0")) < 0);
        assertTrue(Semver.build("1.1.0").compare(Semver.build("1.0.0")) > 0);
    }

    @Test
    public void stableIsGreaterThanPreRelease() {
        assertTrue(Semver.build("1.0.0").compare(Semver.build("1.0.0-alpha")) > 0);
    }
}
