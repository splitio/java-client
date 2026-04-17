package io.split.rules.matchers;

import io.split.rules.model.DataType;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BetweenMatcherTest {

    @Test
    public void matchesValueInRange() {
        BetweenMatcher m = new BetweenMatcher(10L, 20L, DataType.NUMBER);
        assertTrue(m.match(15L, null, null, null));
        assertTrue(m.match(10L, null, null, null));
        assertTrue(m.match(20L, null, null, null));
    }

    @Test
    public void doesNotMatchValueOutOfRange() {
        BetweenMatcher m = new BetweenMatcher(10L, 20L, DataType.NUMBER);
        assertFalse(m.match(9L, null, null, null));
        assertFalse(m.match(21L, null, null, null));
    }

    @Test
    public void doesNotMatchNull() {
        BetweenMatcher m = new BetweenMatcher(10L, 20L, DataType.NUMBER);
        assertFalse(m.match(null, null, null, null));
    }

    @Test
    public void matchesIntegerInRange() {
        BetweenMatcher m = new BetweenMatcher(10L, 20L, DataType.NUMBER);
        assertTrue(m.match(15, null, null, null));
    }
}
