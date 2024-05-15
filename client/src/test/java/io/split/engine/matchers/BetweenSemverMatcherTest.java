package io.split.engine.matchers;

import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

/**
 * Tests for EqualToSemverMatcher
 */

public class BetweenSemverMatcherTest {

    @Test
    public void works() {
        BetweenSemverMatcher betweenSemverMatcher = new BetweenSemverMatcher("2.1.8", "3.0.0");

        assertTrue( betweenSemverMatcher.match("2.1.8", null, null, null));
        assertTrue( betweenSemverMatcher.match("2.1.9", null, null, null));
        assertFalse( betweenSemverMatcher.match("2.1.8-rc", null, null, null));
        assertTrue( betweenSemverMatcher.match("3.0.0+build", null, null, null));
        assertFalse( betweenSemverMatcher.match("4.5.8", null, null, null));
        assertFalse( betweenSemverMatcher.match("1.0.4", null, null, null));
        assertTrue(betweenSemverMatcher.equals(betweenSemverMatcher));
        assertTrue(betweenSemverMatcher.hashCode() != 0);
    }

    @Test
    public void testNull() {
        BetweenSemverMatcher betweenSemverMatcher = new BetweenSemverMatcher("2.1.8", "3.0.0");
        assertFalse( betweenSemverMatcher.match(null, null, null, null));

        betweenSemverMatcher = new BetweenSemverMatcher("2.www.8", "3.xx.0");
        assertFalse(betweenSemverMatcher.match("2.www.8", null, null, null));
    }
}
