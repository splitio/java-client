package io.split.rules.matchers;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AllKeysMatcherTest {

    private final AllKeysMatcher _matcher = new AllKeysMatcher();

    @Test
    public void matchesNonNullValue() {
        assertTrue(_matcher.match("anything", null, null, null));
    }

    @Test
    public void doesNotMatchNull() {
        assertFalse(_matcher.match(null, null, null, null));
    }

    @Test
    public void equalityHolds() {
        assertTrue(_matcher.equals(new AllKeysMatcher()));
    }
}
