package io.split.engine.matchers;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.split.client.dtos.DataType;
import io.split.engine.matchers.strings.WhitelistMatcher;
import org.junit.Test;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Tests for AllKeysMatcher
 */
public class AttributeMatcherTest {

    @Test
    public void works() {
        AttributeMatcher matcher = new AttributeMatcher("creation_date", new GreaterThanOrEqualToMatcher(100L, DataType.NUMBER), false);
        assertThat(matcher.match("ignore", null, ImmutableMap.<String, Object>of("creation_date", 99L), null), is(false));
        assertThat(matcher.match("ignore", null, ImmutableMap.<String, Object>of("creation_date", 100L), null), is(true));
        assertThat(matcher.match("ignore", null, ImmutableMap.<String, Object>of("creation_date", 101), null), is(true));
        assertThat(matcher.match("ignore", null, ImmutableMap.<String, Object>of("creation_date", 101.3), null), is(false));
        assertThat(matcher.match("ignore", null, ImmutableMap.<String, Object>of("creation_date", Calendar.getInstance()), null), is(false));
        assertThat(matcher.match("ignore", null, ImmutableMap.<String, Object>of("creation_date", new Date()), null), is(false));
    }

    @Test
    public void works_negation() {
        AttributeMatcher matcher = new AttributeMatcher("creation_date", new GreaterThanOrEqualToMatcher(100L, DataType.NUMBER), true);
        assertThat(matcher.match("ignore", null, ImmutableMap.<String, Object>of("creation_date", 99L), null), is(true));
        assertThat(matcher.match("ignore", null, ImmutableMap.<String, Object>of("creation_date", 100L), null), is(false));
        assertThat(matcher.match("ignore", null, ImmutableMap.<String, Object>of("creation_date", 101), null), is(false));
        assertThat(matcher.match("ignore", null, ImmutableMap.<String, Object>of("creation_date", 101.3), null), is(true));
        assertThat(matcher.match("ignore", null, ImmutableMap.<String, Object>of("creation_date", Calendar.getInstance()), null), is(true));
        assertThat(matcher.match("ignore", null, ImmutableMap.<String, Object>of("creation_date", new Date()), null), is(true));
    }

    @Test
    public void works_less_than_or_equal_to() {
        AttributeMatcher matcher = new AttributeMatcher("creation_date", new LessThanOrEqualToMatcher(100L, DataType.NUMBER), false);
        assertThat(matcher.match("ignore", null, ImmutableMap.<String, Object>of("creation_date", 99L), null), is(true));
        assertThat(matcher.match("ignore", null, ImmutableMap.<String, Object>of("creation_date", 100L), null), is(true));
        assertThat(matcher.match("ignore", null, ImmutableMap.<String, Object>of("creation_date", 101), null), is(false));
        assertThat(matcher.match("ignore", null, ImmutableMap.<String, Object>of("creation_date", 101.3), null), is(false));
        assertThat(matcher.match("ignore", null, ImmutableMap.<String, Object>of("creation_date", Calendar.getInstance()), null), is(false));
        assertThat(matcher.match("ignore", null, ImmutableMap.<String, Object>of("creation_date", new Date()), null), is(false));
    }

    @Test
    public void works_boolean() {
        AttributeMatcher matcher = new AttributeMatcher("value", new BooleanMatcher(true), false);
        assertThat(matcher.match("ignore", null, ImmutableMap.<String, Object>of("value", true), null), is(true));
        assertThat(matcher.match("ignore", null, ImmutableMap.<String, Object>of("value", "true"), null), is(true));
        assertThat(matcher.match("ignore", null, ImmutableMap.<String, Object>of("value", "True"), null), is(true));
        assertThat(matcher.match("ignore", null, ImmutableMap.<String, Object>of("value", "TrUe"), null), is(true));
        assertThat(matcher.match("ignore", null, ImmutableMap.<String, Object>of("value", "TRUE"), null), is(true));
        assertThat(matcher.match("ignore", null, ImmutableMap.<String, Object>of("value", false), null), is(false));
        assertThat(matcher.match("ignore", null, ImmutableMap.<String, Object>of("value", "false"), null), is(false));
        assertThat(matcher.match("ignore", null, ImmutableMap.<String, Object>of("value", "False"), null), is(false));
        assertThat(matcher.match("ignore", null, ImmutableMap.<String, Object>of("value", "FALSE"), null), is(false));
        assertThat(matcher.match("ignore", null, ImmutableMap.<String, Object>of("value", "faLSE"), null), is(false));
        assertThat(matcher.match("ignore", null, ImmutableMap.<String, Object>of("value", ""), null), is(false));
        assertThat(matcher.match("ignore", null, ImmutableMap.<String, Object>of("value", 0), null), is(false));
        assertThat(matcher.match("ignore", null, ImmutableMap.<String, Object>of("value", 1), null), is(false));
    }

    @Test
    public void error_conditions() {
        AttributeMatcher matcher = new AttributeMatcher("creation_date", new GreaterThanOrEqualToMatcher(100L, DataType.NUMBER), false);
        assertThat(matcher.match("ignore", null, null, null), is(false));
        assertThat(matcher.match("ignore", null, ImmutableMap.<String, Object>of("foo", 101), null), is(false));
        assertThat(matcher.match("ignore", null, ImmutableMap.<String, Object>of("creation_date", "101"), null), is(false));
    }

    @Test
    public void dates() {
        AttributeMatcher matcher = new AttributeMatcher("creation_date", new GreaterThanOrEqualToMatcher(Calendar.getInstance().getTimeInMillis(), DataType.DATETIME), false);

        Calendar c = Calendar.getInstance();
        c.add(Calendar.YEAR, -1);
        assertThat(matcher.match("ignore", null, ImmutableMap.<String, Object>of("creation_date", c.getTimeInMillis()), null), is(false));

        c.add(Calendar.YEAR, 2);
        assertThat(matcher.match("ignore", null, ImmutableMap.<String, Object>of("creation_date", c.getTimeInMillis()), null), is(true));
    }

    @Test
    public void between() {
        AttributeMatcher matcher = new AttributeMatcher("creation_date", new BetweenMatcher(10, 12, DataType.NUMBER), false);

        assertThat(matcher.match("ignore", null, ImmutableMap.<String, Object>of("creation_date", 9), null), is(false));
        assertThat(matcher.match("ignore", null, ImmutableMap.<String, Object>of("creation_date", 10), null), is(true));
        assertThat(matcher.match("ignore", null, ImmutableMap.<String, Object>of("creation_date", 11), null), is(true));
        assertThat(matcher.match("ignore", null, ImmutableMap.<String, Object>of("creation_date", 12), null), is(true));
        assertThat(matcher.match("ignore", null, ImmutableMap.<String, Object>of("creation_date", 13), null), is(false));
    }


    @Test
    public void when_no_attribute_we_use_the_key() {
        AttributeMatcher matcher = new AttributeMatcher(null, new WhitelistMatcher(Lists.newArrayList("trial")), false);

        Map<String, Object> nullMap = Maps.newHashMap();
        nullMap.put("planType", null);

        assertThat(matcher.match("trial", null, ImmutableMap.<String, Object>of("planType", "trial"), null), is(true));
        assertThat(matcher.match("trial", null, ImmutableMap.<String, Object>of("planType", "Trial"), null), is(true));
        assertThat(matcher.match("trial", null, nullMap, null), is(true));
        assertThat(matcher.match("trial", null, ImmutableMap.<String, Object>of("planType", "premium"), null), is(true));
        assertThat(matcher.match("trial", null, ImmutableMap.<String, Object>of("planType", 10), null), is(true));
        assertThat(matcher.match("trial", null, Collections.<String, Object>emptyMap(), null), is(true));
        assertThat(matcher.match("trial", null, null, null), is(true));
        assertThat(matcher.match("premium", null, null, null), is(false));
    }
}
