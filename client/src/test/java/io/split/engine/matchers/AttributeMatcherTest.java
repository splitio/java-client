package io.split.engine.matchers;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.split.client.dtos.DataType;
import io.split.engine.matchers.strings.WhitelistMatcher;
import org.junit.Assert;
import org.junit.Test;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

/**
 * Tests for AllKeysMatcher
 */
public class AttributeMatcherTest {

    @Test
    public void works() {
        AttributeMatcher matcher = new AttributeMatcher("creation_date", new GreaterThanOrEqualToMatcher(100L, DataType.NUMBER), false);
        Assert.assertFalse(matcher.match("ignore", null, ImmutableMap.<String, Object>of("creation_date", 99L), null));
        Assert.assertTrue(matcher.match("ignore", null, ImmutableMap.<String, Object>of("creation_date", 100L), null));
        Assert.assertTrue(matcher.match("ignore", null, ImmutableMap.<String, Object>of("creation_date", 101), null));
        Assert.assertFalse(matcher.match("ignore", null, ImmutableMap.<String, Object>of("creation_date", 101.3), null));
        Assert.assertFalse(matcher.match("ignore", null, ImmutableMap.<String, Object>of("creation_date", Calendar.getInstance()), null));
        Assert.assertFalse(matcher.match("ignore", null, ImmutableMap.<String, Object>of("creation_date", new Date()), null));
    }

    @Test
    public void worksNegation() {
        AttributeMatcher matcher = new AttributeMatcher("creation_date", new GreaterThanOrEqualToMatcher(100L, DataType.NUMBER), true);
        Assert.assertTrue(matcher.match("ignore", null, ImmutableMap.<String, Object>of("creation_date", 99L), null));
        Assert.assertTrue(matcher.match("ignore", null, ImmutableMap.<String, Object>of("creation_date", 99L), null));
        Assert.assertFalse(matcher.match("ignore", null, ImmutableMap.<String, Object>of("creation_date", 100L), null));
        Assert.assertFalse(matcher.match("ignore", null, ImmutableMap.<String, Object>of("creation_date", 101), null));
        Assert.assertTrue(matcher.match("ignore", null, ImmutableMap.<String, Object>of("creation_date", 101.3), null));
        Assert.assertTrue(matcher.match("ignore", null, ImmutableMap.<String, Object>of("creation_date", Calendar.getInstance()), null));
        Assert.assertTrue(matcher.match("ignore", null, ImmutableMap.<String, Object>of("creation_date", new Date()), null));
    }

    @Test
    public void worksLessThanOrEqualTo() {
        AttributeMatcher matcher = new AttributeMatcher("creation_date", new LessThanOrEqualToMatcher(100L, DataType.NUMBER), false);
        Assert.assertTrue(matcher.match("ignore", null, ImmutableMap.<String, Object>of("creation_date", 99L), null));
        Assert.assertTrue(matcher.match("ignore", null, ImmutableMap.<String, Object>of("creation_date", 100L), null));
        Assert.assertFalse(matcher.match("ignore", null, ImmutableMap.<String, Object>of("creation_date", 101), null));
        Assert.assertFalse(matcher.match("ignore", null, ImmutableMap.<String, Object>of("creation_date", 101.3), null));
        Assert.assertFalse(matcher.match("ignore", null, ImmutableMap.<String, Object>of("creation_date", 101.3), null));
        Assert.assertFalse(matcher.match("ignore", null, ImmutableMap.<String, Object>of("creation_date", Calendar.getInstance()), null));
        Assert.assertFalse(matcher.match("ignore", null, ImmutableMap.<String, Object>of("creation_date", new Date()), null));
    }

    @Test
    public void worksBoolean() {
        AttributeMatcher matcher = new AttributeMatcher("value", new BooleanMatcher(true), false);
        Assert.assertTrue(matcher.match("ignore", null, ImmutableMap.<String, Object>of("value", true), null));
        Assert.assertTrue(matcher.match("ignore", null, ImmutableMap.<String, Object>of("value", "true"), null));
        Assert.assertTrue(matcher.match("ignore", null, ImmutableMap.<String, Object>of("value", "True"), null));
        Assert.assertTrue(matcher.match("ignore", null, ImmutableMap.<String, Object>of("value", "TrUe"), null));
        Assert.assertTrue(matcher.match("ignore", null, ImmutableMap.<String, Object>of("value", "TRUE"), null));
        Assert.assertFalse(matcher.match("ignore", null, ImmutableMap.<String, Object>of("value", false), null));
        Assert.assertFalse(matcher.match("ignore", null, ImmutableMap.<String, Object>of("value", "false"), null));
        Assert.assertFalse(matcher.match("ignore", null, ImmutableMap.<String, Object>of("value", "False"), null));
        Assert.assertFalse(matcher.match("ignore", null, ImmutableMap.<String, Object>of("value", "FALSE"), null));
        Assert.assertFalse(matcher.match("ignore", null, ImmutableMap.<String, Object>of("value", "faLSE"), null));
        Assert.assertFalse(matcher.match("ignore", null, ImmutableMap.<String, Object>of("value", ""), null));
        Assert.assertFalse(matcher.match("ignore", null, ImmutableMap.<String, Object>of("value", 0), null));
        Assert.assertFalse(matcher.match("ignore", null, ImmutableMap.<String, Object>of("value", 1), null));
    }

    @Test
    public void errorConditions() {
        AttributeMatcher matcher = new AttributeMatcher("creation_date", new GreaterThanOrEqualToMatcher(100L, DataType.NUMBER), false);
        Assert.assertFalse(matcher.match("ignore", null, null, null));
        Assert.assertFalse(matcher.match("ignore", null, ImmutableMap.<String, Object>of("foo", 101), null));
        Assert.assertFalse(matcher.match("ignore", null, ImmutableMap.<String, Object>of("creation_date", "101"), null));
    }

    @Test
    public void dates() {
        AttributeMatcher matcher = new AttributeMatcher("creation_date", new GreaterThanOrEqualToMatcher(Calendar.getInstance().getTimeInMillis(), DataType.DATETIME), false);

        Calendar c = Calendar.getInstance();
        c.add(Calendar.YEAR, -1);
        Assert.assertFalse(matcher.match("ignore", null, ImmutableMap.<String, Object>of("creation_date", c.getTimeInMillis()), null));

        c.add(Calendar.YEAR, 2);
        Assert.assertTrue(matcher.match("ignore", null, ImmutableMap.<String, Object>of("creation_date", c.getTimeInMillis()), null));
    }

    @Test
    public void between() {
        AttributeMatcher matcher = new AttributeMatcher("creation_date", new BetweenMatcher(10, 12, DataType.NUMBER), false);

        Assert.assertFalse(matcher.match("ignore", null, ImmutableMap.<String, Object>of("creation_date", 9), null));
        Assert.assertTrue(matcher.match("ignore", null, ImmutableMap.<String, Object>of("creation_date", 10), null));
        Assert.assertTrue(matcher.match("ignore", null, ImmutableMap.<String, Object>of("creation_date", 11), null));
        Assert.assertTrue(matcher.match("ignore", null, ImmutableMap.<String, Object>of("creation_date", 12), null));
        Assert.assertFalse(matcher.match("ignore", null, ImmutableMap.<String, Object>of("creation_date", 13), null));
    }


    @Test
    public void whenNoAttributeWeUseTheKey() {
        AttributeMatcher matcher = new AttributeMatcher(null, new WhitelistMatcher(Lists.newArrayList("trial")), false);

        Map<String, Object> nullMap = Maps.newHashMap();
        nullMap.put("planType", null);

        Assert.assertTrue(matcher.match("trial", null, ImmutableMap.<String, Object>of("planType", "trial"), null));
        Assert.assertTrue(matcher.match("trial", null, ImmutableMap.<String, Object>of("planType", "Trial"), null));
        Assert.assertTrue(matcher.match("trial", null, nullMap, null));
        Assert.assertTrue(matcher.match("trial", null, ImmutableMap.<String, Object>of("planType", "premium"), null));
        Assert.assertTrue(matcher.match("trial", null, ImmutableMap.<String, Object>of("planType", 10), null));
        Assert.assertTrue(matcher.match("trial", null, Collections.<String, Object>emptyMap(), null));
        Assert.assertTrue(matcher.match("trial", null, null, null));
        Assert.assertFalse(matcher.match("premium", null, null, null));
    }
}