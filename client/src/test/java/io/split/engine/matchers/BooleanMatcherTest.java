package io.split.engine.matchers;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class BooleanMatcherTest {

    @Test
    public void works_true() {
        BooleanMatcher matcher = new BooleanMatcher(true);
        assertThat(matcher.match(null, null, null, null), is(false));
        assertThat(matcher.match(true, null, null, null), is(true));
        assertThat(matcher.match("true", null, null, null), is(true));
        assertThat(matcher.match(Boolean.TRUE, null, null, null), is(true));
        assertThat(matcher.match(false, null, null, null), is(false));
        assertThat(matcher.match("false", null, null, null), is(false));
        assertThat(matcher.match(Boolean.FALSE, null, null, null), is(false));
        assertThat(matcher.match(0, null, null, null), is(false));
        assertThat(matcher.match(1, null, null, null), is(false));
        assertThat(matcher.match("word", null, null, null), is(false));
    }

    @Test
    public void works_false() {
        BooleanMatcher matcher = new BooleanMatcher(false);
        assertThat(matcher.match(null, null, null, null), is(false));
        assertThat(matcher.match(true, null, null, null), is(false));
        assertThat(matcher.match("true", null, null, null), is(false));
        assertThat(matcher.match(Boolean.TRUE, null, null, null), is(false));
        assertThat(matcher.match(false, null, null, null), is(true));
        assertThat(matcher.match("false", null, null, null), is(true));
        assertThat(matcher.match(Boolean.FALSE, null, null, null), is(true));
        assertThat(matcher.match(0, null, null, null), is(false));
        assertThat(matcher.match(1, null, null, null), is(false));
        assertThat(matcher.match("word", null, null, null), is(false));
    }
}