package io.split.engine.matchers;

import io.split.client.dtos.DataType;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Tests for AllKeysMatcher
 */
public class LessThanOrEqualToMatcherTest {

    @Test
    public void works() {
        LessThanOrEqualToMatcher matcher = new LessThanOrEqualToMatcher(10, DataType.NUMBER);
        assertThat(matcher.match(null), is(false));
        assertThat(matcher.match(1), is(true));
        assertThat(matcher.match(new Long(-1)), is(true));
        assertThat(matcher.match(9), is(true));
        assertThat(matcher.match(new Long(10)), is(true));
        assertThat(matcher.match(11), is(false));
        assertThat(matcher.match(100), is(false));
    }

    @Test
    public void works_negative() {
        LessThanOrEqualToMatcher matcher = new LessThanOrEqualToMatcher(-10, DataType.NUMBER);
        assertThat(matcher.match(null), is(false));
        assertThat(matcher.match(1), is(false));
        assertThat(matcher.match(new Long(-1)), is(false));
        assertThat(matcher.match(9), is(false));
        assertThat(matcher.match(new Long(10)), is(false));
        assertThat(matcher.match(11), is(false));
        assertThat(matcher.match(-9), is(false));
        assertThat(matcher.match(-10), is(true));
        assertThat(matcher.match(-11), is(true));
    }

    @Test
    public void works_dates() {
        long april11_2016_23_59 = 1460419199000L;
        long april12_2016_midnight_19 = 1460420360000L;
        long april12_2016_midnight_20 = 1460420421903L;
        long april12_2016_midnight_20_59 = 1460420459000L;
        long april12_2016_1_20 = 1460424039000L;

        LessThanOrEqualToMatcher matcher = new LessThanOrEqualToMatcher(april12_2016_midnight_20, DataType.DATETIME);
        assertThat(matcher.match(april11_2016_23_59), is(true));
        assertThat(matcher.match(april12_2016_midnight_19), is(true));
        assertThat(matcher.match(april12_2016_midnight_20), is(true));
        assertThat(matcher.match(april12_2016_midnight_20_59), is(true));
        assertThat(matcher.match(april12_2016_1_20), is(false));
    }


}
