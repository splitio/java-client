package io.split.engine.matchers;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Tests for EqualToSemverMatcher
 */

public class BetweenSemverMatcherTest {

    @Test
    public void works() {
        BetweenSemverMatcher betweenSemverMatcher = new BetweenSemverMatcher("2.1.8", "3.0.0");

        assertTrue( betweenSemverMatcher.match("2.1.8", null, null, null) == true);
        assertTrue( betweenSemverMatcher.match("2.1.9", null, null, null) == true);
        assertTrue( betweenSemverMatcher.match("2.1.8-rc", null, null, null) == false);
        assertTrue( betweenSemverMatcher.match("3.0.0+build", null, null, null) == true);
    }
}
