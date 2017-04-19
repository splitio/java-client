package io.split.engine.matchers.strings;

import io.split.engine.matchers.strings.StartsWithAnyOfMatcher;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created by adilaijaz on 4/18/17.
 */
public class StartsWithAnyOfMatcherTest {
    @Test
    public void works_for_sets() {
        Set<String> set = new HashSet<>();
        set.add("first");
        set.add("second");

        StartsWithAnyOfMatcher matcher = new StartsWithAnyOfMatcher(set);

        works(matcher);
    }

    @Test
    public void works_for_lists() {
        List<String> list = new ArrayList<>();
        list.add("first");
        list.add("second");

        StartsWithAnyOfMatcher matcher = new StartsWithAnyOfMatcher(list);

        works(matcher);
    }

    private void works(StartsWithAnyOfMatcher matcher) {
        assertThat(matcher.match(null), is(false));
        assertThat(matcher.match(""), is(false));
        assertThat(matcher.match("foo"), is(false));
        assertThat(matcher.match("firstsecond"), is(true));
        assertThat(matcher.match("firt"), is(false));
        assertThat(matcher.match("secondfirst"), is(true));
    }


    @Test
    public void works_for_empty_paramter() {
        List<String> list = new ArrayList<>();

        StartsWithAnyOfMatcher matcher = new StartsWithAnyOfMatcher(list);

        assertThat(matcher.match(null), is(false));
        assertThat(matcher.match(""), is(false));
        assertThat(matcher.match("foo"), is(false));
        assertThat(matcher.match("firstsecond"), is(false));
        assertThat(matcher.match("firt"), is(false));
        assertThat(matcher.match("secondfirst"), is(false));
    }
}
