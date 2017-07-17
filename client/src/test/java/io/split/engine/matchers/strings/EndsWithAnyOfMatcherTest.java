package io.split.engine.matchers.strings;

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
public class EndsWithAnyOfMatcherTest {
    @Test
    public void works_for_sets() {
        Set<String> set = new HashSet<>();
        set.add("first");
        set.add("second");

        EndsWithAnyOfMatcher matcher = new EndsWithAnyOfMatcher(set);

        works(matcher);
    }

    @Test
    public void works_for_lists() {
        List<String> list = new ArrayList<>();
        list.add("first");
        list.add("second");

        EndsWithAnyOfMatcher matcher = new EndsWithAnyOfMatcher(list);

        works(matcher);
    }

    private void works(EndsWithAnyOfMatcher matcher) {
        assertThat(matcher.match(null, null, null, null), is(false));
        assertThat(matcher.match("", null, null, null), is(false));
        assertThat(matcher.match("foo", null, null, null), is(false));
        assertThat(matcher.match("secondfirst", null, null, null), is(true));
        assertThat(matcher.match("first", null, null, null), is(true));
        assertThat(matcher.match("second", null, null, null), is(true));
    }


    @Test
    public void works_for_empty_paramter() {
        List<String> list = new ArrayList<>();

        EndsWithAnyOfMatcher matcher = new EndsWithAnyOfMatcher(list);

        assertThat(matcher.match(null, null, null, null), is(false));
        assertThat(matcher.match("", null, null, null), is(false));
        assertThat(matcher.match("foo", null, null, null), is(false));
        assertThat(matcher.match("firstsecond", null, null, null), is(false));
        assertThat(matcher.match("firt", null, null, null), is(false));
        assertThat(matcher.match("secondfirst", null, null, null), is(false));
    }
}
