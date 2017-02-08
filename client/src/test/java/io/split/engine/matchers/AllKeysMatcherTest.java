package io.split.engine.matchers;

import io.split.engine.matchers.AllKeysMatcher;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Tests for AllKeysMatcher
 */
public class AllKeysMatcherTest {

    @Test
    public void works() {
        AllKeysMatcher matcher = new AllKeysMatcher();
        assertThat(matcher.match(null), is(false));
        for (int i = 0; i < 100; i++) {
            String randomKey = RandomStringUtils.random(10);
            assertThat(matcher.match(randomKey), is(true));
        }

    }

}
