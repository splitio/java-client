package io.split.engine.matchers.strings;

import com.google.common.collect.Lists;
import io.split.engine.matchers.strings.WhitelistMatcher;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Tests for WhitelistMatcher
 *
 * @author adil
 */
public class WhitelistMatcherTest {

    @Test
    public void works() {
        List<String> whitelist = Lists.newArrayList("a", "a\"b");

        WhitelistMatcher matcher = new WhitelistMatcher(whitelist);

        for (String item : whitelist) {
            assertThat(matcher.match(item), is(true));
        }

        assertThat(matcher.match("hello"), is(false));
        assertThat(matcher.match(null), is(false));
    }

    @Test
    public void works_with_empty_whitelist() {
        List<String> whitelist = Lists.newArrayList("a", "a\"b");

        WhitelistMatcher matcher = new WhitelistMatcher(Collections.<String>emptyList());

        for (String item : whitelist) {
            assertThat(matcher.match(item), is(false));
        }

    }

}
