package io.split.engine.matchers;

import io.split.client.dtos.DataType;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Tests for AllKeysMatcher
 */
public class GreaterThanOrEqualToMatcherTest {

    @Test
    public void works() {
        GreaterThanOrEqualToMatcher matcher = new GreaterThanOrEqualToMatcher(10, DataType.NUMBER);
        assertThat(matcher.match(null, null, null, null), is(false));
        assertThat(matcher.match(1, null, null, null), is(false));
        assertThat(matcher.match(new Long(-1), null, null, null), is(false));
        assertThat(matcher.match(9, null, null, null), is(false));
        assertThat(matcher.match(new Long(10), null, null, null), is(true));
        assertThat(matcher.match(11, null, null, null), is(true));
        assertThat(matcher.match(100, null, null, null), is(true));
    }

    @Test
    public void works_negative() {
        GreaterThanOrEqualToMatcher matcher = new GreaterThanOrEqualToMatcher(-10, DataType.NUMBER);
        assertThat(matcher.match(null, null, null, null), is(false));
        assertThat(matcher.match(1, null, null, null), is(true));
        assertThat(matcher.match(new Long(-1), null, null, null), is(true));
        assertThat(matcher.match(9, null, null, null), is(true));
        assertThat(matcher.match(new Long(10), null, null, null), is(true));
        assertThat(matcher.match(11, null, null, null), is(true));
        assertThat(matcher.match(100, null, null, null), is(true));
        assertThat(matcher.match(-10, null, null, null), is(true));
        assertThat(matcher.match(-11, null, null, null), is(false));
    }

    @Test
    public void works_dates() {
        long april12_2016_midnight_19 = 1460420360000L;
        long april12_2016_midnight_20 = 1460420421903L;
        long april12_2016_midnight_20_59 = 1460420459000L;
        long april12_2016_1_20 = 1460424039000L;
        long april12_2016_18_20 = 1460485239000L;

        GreaterThanOrEqualToMatcher matcher = new GreaterThanOrEqualToMatcher(april12_2016_midnight_20, DataType.DATETIME);
        assertThat(matcher.match(april12_2016_midnight_19, null, null, null), is(false));
        assertThat(matcher.match(april12_2016_midnight_20_59, null, null, null), is(true));
        assertThat(matcher.match(april12_2016_midnight_20, null, null, null), is(true));
        assertThat(matcher.match(april12_2016_1_20, null, null, null), is(true));
        assertThat(matcher.match(april12_2016_18_20, null, null, null), is(true));
    }

}
