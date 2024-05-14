package io.split.engine.matchers;

import com.google.common.collect.Lists;
import io.split.client.dtos.MatcherCombiner;
import io.split.engine.matchers.strings.WhitelistMatcher;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

/**
 * Tests CombiningMatcher
 *
 * @author adil
 */
public class CombiningMatcherTest {

    @Test
    public void worksAnd() {
        AttributeMatcher matcher1 = AttributeMatcher.vanilla(new AllKeysMatcher());
        AttributeMatcher matcher2 = AttributeMatcher.vanilla(new WhitelistMatcher(Lists.newArrayList("a", "b")));

        CombiningMatcher combiner = new CombiningMatcher(MatcherCombiner.AND, Lists.newArrayList(matcher1, matcher2));

        Assert.assertTrue(combiner.match("a", null, null, null));
        Assert.assertTrue(combiner.match("b", null, Collections.<String, Object>emptyMap(), null));
        Assert.assertFalse(combiner.match("c", null, null, null));
    }
}