package io.split.engine.matchers;

import io.split.engine.matchers.EqualToMatcherSemver;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Tests for EqualToSemverMatcher
 */

public class EqualToSemverTest {

    @Test
    public void works() {
        EqualToMatcherSemver equalToMatcherSemver = new EqualToMatcherSemver("2.1.8");

        assertTrue( equalToMatcherSemver.match("2.1.8", null, null, null) == true);
        assertTrue( equalToMatcherSemver.match("2.1.9", null, null, null) == false);
        assertTrue( equalToMatcherSemver.match("2.1.8-rc", null, null, null) == false);
        assertTrue( equalToMatcherSemver.match("2.1.8+build", null, null, null) == false);
    }
}
