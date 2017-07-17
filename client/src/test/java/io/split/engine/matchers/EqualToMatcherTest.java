package io.split.engine.matchers;

import io.split.client.dtos.DataType;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Tests for AllKeysMatcher
 */
public class EqualToMatcherTest {

    @Test
    public void works() {
        EqualToMatcher matcher = new EqualToMatcher(10, DataType.NUMBER);
        assertThat(matcher.match(null, null, null, null), is(false));
        assertThat(matcher.match(1, null, null, null), is(false));
        assertThat(matcher.match(new Long(-1), null, null, null), is(false));
        assertThat(matcher.match(9, null, null, null), is(false));
        assertThat(matcher.match(new Long(10), null, null, null), is(true));
        assertThat(matcher.match(11, null, null, null), is(false));
        assertThat(matcher.match(100, null, null, null), is(false));
    }

    @Test
    public void works_negative() {
        EqualToMatcher matcher = new EqualToMatcher(-10, DataType.NUMBER);
        assertThat(matcher.match(null, null, null, null), is(false));
        assertThat(matcher.match(1, null, null, null), is(false));
        assertThat(matcher.match(new Long(-1), null, null, null), is(false));
        assertThat(matcher.match(9, null, null, null), is(false));
        assertThat(matcher.match(new Long(10), null, null, null), is(false));
        assertThat(matcher.match(11, null, null, null), is(false));
        assertThat(matcher.match(-10, null, null, null), is(true));
        assertThat(matcher.match(-11, null, null, null), is(false));
    }

    @Test
    public void works_dates() {
        long april11_2016_23_59_59 = 1460419199000L;
        long april12_2016_midnight_19 = 1460420360000L;
        long april12_2016_midnight_20 = 1460420421903L;
        long april12_2016_1_20 = 1460424039000L;
        long april13_2016_00_00_00 = 1460505600000L;

        EqualToMatcher matcher = new EqualToMatcher(april12_2016_midnight_20, DataType.DATETIME);
        assertThat(matcher.match(april11_2016_23_59_59, null, null, null), is(false));
        assertThat(matcher.match(april12_2016_midnight_19, null, null, null), is(true));
        assertThat(matcher.match(april12_2016_midnight_20, null, null, null), is(true));
        assertThat(matcher.match(april12_2016_1_20, null, null, null), is(true));
        assertThat(matcher.match(april13_2016_00_00_00, null, null, null), is(false));

    }

}
