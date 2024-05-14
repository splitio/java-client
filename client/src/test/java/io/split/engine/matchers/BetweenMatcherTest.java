package io.split.engine.matchers;

import io.split.client.dtos.DataType;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for BetweenMatcherTest
 */
public class BetweenMatcherTest {

    @Test
    public void works() {
        int start = -3;
        int end = 20;

        BetweenMatcher matcher = new BetweenMatcher(start, end, DataType.NUMBER);

        Assert.assertFalse(matcher.match(null, null, null, null));

        for (int i = start; i <= end; i++) {
            Assert.assertTrue(matcher.match(i, null, null, null));
        }
        Assert.assertFalse(matcher.match(new Long(start - 1), null, null, null));
        Assert.assertFalse(matcher.match(end + 1, null, null, null));
    }


    @Test
    public void worksDates() {
        long april11_2016_23_59 = 1460419199000L;
        long april12_2016_midnight_19 = 1460420360000L;
        long april12_2016_midnight_20 = 1460420421903L;
        long april12_2016_midnight_20_59 = 1460420459000L;
        long april12_2016_1_20 = 1460424039000L;


        BetweenMatcher matcher = new BetweenMatcher(april12_2016_midnight_19, april12_2016_midnight_20_59, DataType.DATETIME);

        Assert.assertFalse(matcher.match(april11_2016_23_59, null, null, null));
        Assert.assertTrue(matcher.match(april12_2016_midnight_19, null, null, null));
        Assert.assertTrue(matcher.match(april12_2016_midnight_20, null, null, null));
        Assert.assertTrue(matcher.match(april12_2016_midnight_20_59, null, null, null));
        Assert.assertFalse(matcher.match(april12_2016_1_20, null, null, null));
    }
}