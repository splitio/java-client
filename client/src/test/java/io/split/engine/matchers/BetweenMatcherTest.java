package io.split.engine.matchers;

import io.split.client.dtos.DataType;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Tests for BetweenMatcherTest
 */
public class BetweenMatcherTest {

    @Test
    public void works() {
        int start = -3;
        int end = 20;

        BetweenMatcher matcher = new BetweenMatcher(start, end, DataType.NUMBER);

        assertThat(matcher.match(null, null, null, null), is(false));

        for (int i = start; i <= end; i++) {
            assertThat(matcher.match(i, null, null, null), is(true));
        }
        assertThat(matcher.match(new Long(start - 1), null, null, null), is(false));
        assertThat(matcher.match(end + 1, null, null, null), is(false));
    }


    @Test
    public void works_dates() {
        long april11_2016_23_59 = 1460419199000L;
        long april12_2016_midnight_19 = 1460420360000L;
        long april12_2016_midnight_20 = 1460420421903L;
        long april12_2016_midnight_20_59 = 1460420459000L;
        long april12_2016_1_20 = 1460424039000L;


        BetweenMatcher matcher = new BetweenMatcher(april12_2016_midnight_19, april12_2016_midnight_20_59, DataType.DATETIME);

        assertThat(matcher.match(april11_2016_23_59, null, null, null), is(false));
        assertThat(matcher.match(april12_2016_midnight_19, null, null, null), is(true));
        assertThat(matcher.match(april12_2016_midnight_20, null, null, null), is(true));
        assertThat(matcher.match(april12_2016_midnight_20_59, null, null, null), is(true));
        assertThat(matcher.match(april12_2016_1_20, null, null, null), is(false));


    }


}
