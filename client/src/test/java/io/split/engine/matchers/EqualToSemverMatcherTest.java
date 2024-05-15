package io.split.engine.matchers;

import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

/**
 * Tests for EqualToSemverMatcher
 */

public class EqualToSemverMatcherTest {

    @Test
    public void works() {
        EqualToSemverMatcher equalToSemverMatcher = new EqualToSemverMatcher("2.1.8");

        assertTrue( equalToSemverMatcher.match("2.1.8", null, null, null));
        assertFalse(equalToSemverMatcher.match("2.1.9", null, null, null));
        assertFalse(equalToSemverMatcher.match("2.1.8-rc", null, null, null));
        assertFalse( equalToSemverMatcher.match("2.1.8+build", null, null, null));
        assertTrue(equalToSemverMatcher.equals(equalToSemverMatcher));
        assertTrue(equalToSemverMatcher.hashCode() != 0);
    }

    @Test
    public void testNull() {
        EqualToSemverMatcher equalToSemverMatcher = new EqualToSemverMatcher("2.1.8");
        assertFalse( equalToSemverMatcher.match(null, null, null, null));

        equalToSemverMatcher = new EqualToSemverMatcher("2.ee.8");
        assertFalse(equalToSemverMatcher.match("2.ee.8", null, null, null));
    }
}
