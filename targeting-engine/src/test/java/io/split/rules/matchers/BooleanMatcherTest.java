package io.split.rules.matchers;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BooleanMatcherTest {

    @Test
    public void matchesTrueWhenValueIsTrue() {
        BooleanMatcher m = new BooleanMatcher(true);
        assertTrue(m.match(true, null, null, null));
        assertTrue(m.match("true", null, null, null));
    }

    @Test
    public void matchesFalseWhenValueIsFalse() {
        BooleanMatcher m = new BooleanMatcher(false);
        assertTrue(m.match(false, null, null, null));
        assertTrue(m.match("false", null, null, null));
    }

    @Test
    public void doesNotMatchOpposite() {
        assertTrue(new BooleanMatcher(true).match(true, null, null, null));
        assertFalse(new BooleanMatcher(true).match(false, null, null, null));
    }

    @Test
    public void doesNotMatchNull() {
        assertFalse(new BooleanMatcher(true).match(null, null, null, null));
    }
}
