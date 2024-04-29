package io.split.engine.matchers;

import com.google.common.collect.Lists;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * Tests for EqualToSemverMatcher
 */

public class InListSemverMatcherTest {

    @Test
    public void works() {
        List<String> whitelist = Lists.newArrayList("2.1.8", "3.4.0");
        InListSemverMatcher inListSemverMatcher = new InListSemverMatcher(whitelist);

        assertTrue( inListSemverMatcher.match("2.1.8", null, null, null) == true);
        assertTrue( inListSemverMatcher.match("2.1.9", null, null, null) == false);
        assertTrue( inListSemverMatcher.match("2.1.8-rc", null, null, null) == false);
        assertTrue( inListSemverMatcher.match("3.4.0", null, null, null) == true);
        assertTrue( inListSemverMatcher.match("3.4.0+build", null, null, null) == false);
    }
}
