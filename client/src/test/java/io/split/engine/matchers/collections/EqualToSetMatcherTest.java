package io.split.engine.matchers.collections;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created by adilaijaz on 4/18/17.
 */
public class EqualToSetMatcherTest {
    @Test
    public void works_for_sets() {
        Set<String> set = new HashSet<>();
        set.add("first");
        set.add("second");

        EqualToSetMatcher matcher = new EqualToSetMatcher(set);

        assertThat(matcher.match(null), is(false));

        Set<String> argument = new HashSet<>();
        assertThat(matcher.match(argument), is(false));

        argument.add("second");
        assertThat(matcher.match(argument), is(false));

        argument.add("first");
        assertThat(matcher.match(argument), is(true));

        argument.add("third");
        assertThat(matcher.match(argument), is(false));
    }

    @Test
    public void works_for_lists() {
        List<String> list = new ArrayList<>();
        list.add("first");
        list.add("second");

        EqualToSetMatcher matcher = new EqualToSetMatcher(list);

        assertThat(matcher.match(null), is(false));

        List<String> argument = new ArrayList<>();
        assertThat(matcher.match(argument), is(false));

        argument.add("second");
        assertThat(matcher.match(argument), is(false));

        argument.add("first");
        assertThat(matcher.match(argument), is(true));

        argument.add("third");
        assertThat(matcher.match(argument), is(false));
    }

    @Test
    public void works_for_empty_paramter() {
        List<String> list = new ArrayList<>();

        EqualToSetMatcher matcher = new EqualToSetMatcher(list);

        assertThat(matcher.match(null), is(false));

        List<String> argument = new ArrayList<>();
        assertThat(matcher.match(argument), is(false));

        argument.add("second");
        assertThat(matcher.match(argument), is(false));

        argument.add("first");
        assertThat(matcher.match(argument), is(false));

        argument.add("third");
        assertThat(matcher.match(argument), is(false));
    }
}
