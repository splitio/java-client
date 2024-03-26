package io.split.engine.matchers;

import io.split.client.dtos.DataType;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for AllKeysMatcher
 */
public class EqualToMatcherTest {

    @Test
    public void works() {
        EqualToMatcher matcher = new EqualToMatcher(10, DataType.NUMBER);
        Assert.assertFalse(matcher.match(null, null, null, null));
        Assert.assertFalse(matcher.match(1, null, null, null));
        Assert.assertFalse(matcher.match(new Long(-1), null, null, null));
        Assert.assertFalse(matcher.match(9, null, null, null));
        Assert.assertTrue(matcher.match(new Long(10), null, null, null));
        Assert.assertFalse(matcher.match(11, null, null, null));
        Assert.assertFalse(matcher.match(100, null, null, null));
    }

    @Test
    public void worksNegative() {
        EqualToMatcher matcher = new EqualToMatcher(-10, DataType.NUMBER);
        Assert.assertFalse(matcher.match(null, null, null, null));
        Assert.assertFalse(matcher.match(1, null, null, null));
        Assert.assertFalse(matcher.match(new Long(-1), null, null, null));
        Assert.assertFalse(matcher.match(9, null, null, null));
        Assert.assertFalse(matcher.match(new Long(10), null, null, null));
        Assert.assertFalse(matcher.match(11, null, null, null));
        Assert.assertTrue(matcher.match(-10, null, null, null));
        Assert.assertFalse(matcher.match(-11, null, null, null));
    }

    @Test
    public void worksDates() {
        long april11_2016_23_59_59 = 1460419199000L;
        long april12_2016_midnight_19 = 1460420360000L;
        long april12_2016_midnight_20 = 1460420421903L;
        long april12_2016_1_20 = 1460424039000L;
        long april13_2016_00_00_00 = 1460505600000L;

        EqualToMatcher matcher = new EqualToMatcher(april12_2016_midnight_20, DataType.DATETIME);
        Assert.assertFalse(matcher.match(april11_2016_23_59_59, null, null, null));
        Assert.assertTrue(matcher.match(april12_2016_midnight_19, null, null, null));
        Assert.assertTrue(matcher.match(april12_2016_midnight_20, null, null, null));
        Assert.assertTrue(matcher.match(april12_2016_1_20, null, null, null));
        Assert.assertFalse(matcher.match(april13_2016_00_00_00, null, null, null));
    }
}