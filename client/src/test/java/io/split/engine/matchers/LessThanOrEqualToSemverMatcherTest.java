package io.split.engine.matchers;

import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

/**
 * Tests for EqualToSemverMatcher
 */

public class LessThanOrEqualToSemverMatcherTest {

    @Test
    public void works() {
        LessThanOrEqualToSemverMatcher lessThanOrEqualToSemverMatcher = new LessThanOrEqualToSemverMatcher("2.1.8");

        assertTrue( lessThanOrEqualToSemverMatcher.match("2.1.8", null, null, null));
        assertFalse( lessThanOrEqualToSemverMatcher.match("2.1.9", null, null, null));
        assertTrue( lessThanOrEqualToSemverMatcher.match("2.1.8-rc", null, null, null));
        assertTrue( lessThanOrEqualToSemverMatcher.match("2.0.10", null, null, null));
        assertTrue( lessThanOrEqualToSemverMatcher.match("2.1.8+build", null, null, null));
        assertTrue(lessThanOrEqualToSemverMatcher.equals(lessThanOrEqualToSemverMatcher));
        assertTrue(lessThanOrEqualToSemverMatcher.hashCode() != 0);
    }

    @Test
    public void testNull() {
        LessThanOrEqualToSemverMatcher lessThanOrEqualToSemverMatcher = new LessThanOrEqualToSemverMatcher("2.1.8");
        assertFalse( lessThanOrEqualToSemverMatcher.match(null, null, null, null));

        lessThanOrEqualToSemverMatcher = new LessThanOrEqualToSemverMatcher("2.ee.8");
        assertFalse(lessThanOrEqualToSemverMatcher.match("2.ee.8", null, null, null));
    }
}
