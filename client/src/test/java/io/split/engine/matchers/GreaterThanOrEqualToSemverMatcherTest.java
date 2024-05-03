package io.split.engine.matchers;

import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

/**
 * Tests for EqualToSemverMatcher
 */

public class GreaterThanOrEqualToSemverMatcherTest {

    @Test
    public void works() {
        GreaterThanOrEqualToSemverMatcher greaterThanOrEqualToSemverMatcher = new GreaterThanOrEqualToSemverMatcher("2.1.8");

        assertTrue( greaterThanOrEqualToSemverMatcher.match("2.1.8", null, null, null));
        assertTrue( greaterThanOrEqualToSemverMatcher.match("2.1.9", null, null, null));
        assertFalse( greaterThanOrEqualToSemverMatcher.match("2.1.8-rc", null, null, null));
        assertFalse( greaterThanOrEqualToSemverMatcher.match("2.0.10", null, null, null));
        assertTrue( greaterThanOrEqualToSemverMatcher.match("2.1.8+build", null, null, null));
    }
}
