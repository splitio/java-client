package io.split.engine.matchers;

import com.google.common.collect.Lists;
import io.split.client.dtos.MatcherCombiner;
import io.split.engine.matchers.strings.WhitelistMatcher;
import org.junit.Test;

import java.util.Collections;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Tests CombiningMatcher
 *
 * @author adil
 */
public class CombiningMatcherTest {

    @Test
    public void works_and() {
        AttributeMatcher matcher1 = AttributeMatcher.vanilla(new AllKeysMatcher());
        AttributeMatcher matcher2 = AttributeMatcher.vanilla(new WhitelistMatcher(Lists.newArrayList("a", "b")));

        CombiningMatcher combiner = new CombiningMatcher(MatcherCombiner.AND, Lists.newArrayList(matcher1, matcher2));

        assertThat(combiner.match("a", null, null, null), is(true));
        assertThat(combiner.match("b", null, Collections.<String, Object>emptyMap(), null), is(true));
        assertThat(combiner.match("c", null, null, null), is(false));
    }

}
