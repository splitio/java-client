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
        assertThat(matcher.match("ignore", ImmutableMap.<String, Object>of("creation_date", 99L)), is(false));
        assertThat(matcher.match("ignore", ImmutableMap.<String, Object>of("creation_date", 100L)), is(true));
        assertThat(matcher.match("ignore", ImmutableMap.<String, Object>of("creation_date", 101)), is(true));
        assertThat(matcher.match("ignore", ImmutableMap.<String, Object>of("creation_date", 101.3)), is(false));
        assertThat(matcher.match("ignore", ImmutableMap.<String, Object>of("creation_date", Calendar.getInstance())), is(false));
        assertThat(matcher.match("ignore", ImmutableMap.<String, Object>of("creation_date", new Date())), is(false));
    }

    @Test
    public void works_negation() {
        AttributeMatcher matcher = new AttributeMatcher("creation_date", new GreaterThanOrEqualToMatcher(100L, DataType.NUMBER), true);
        assertThat(matcher.match("ignore", ImmutableMap.<String, Object>of("creation_date", 99L)), is(true));
        assertThat(matcher.match("ignore", ImmutableMap.<String, Object>of("creation_date", 100L)), is(false));
        assertThat(matcher.match("ignore", ImmutableMap.<String, Object>of("creation_date", 101)), is(false));
        assertThat(matcher.match("ignore", ImmutableMap.<String, Object>of("creation_date", 101.3)), is(true));
        assertThat(matcher.match("ignore", ImmutableMap.<String, Object>of("creation_date", Calendar.getInstance())), is(true));
        assertThat(matcher.match("ignore", ImmutableMap.<String, Object>of("creation_date", new Date())), is(true));
    }

    @Test
    public void works_less_than_or_equal_to() {
        AttributeMatcher matcher = new AttributeMatcher("creation_date", new LessThanOrEqualToMatcher(100L, DataType.NUMBER), false);
        assertThat(matcher.match("ignore", ImmutableMap.<String, Object>of("creation_date", 99L)), is(true));
        assertThat(matcher.match("ignore", ImmutableMap.<String, Object>of("creation_date", 100L)), is(true));
        assertThat(matcher.match("ignore", ImmutableMap.<String, Object>of("creation_date", 101)), is(false));
        assertThat(matcher.match("ignore", ImmutableMap.<String, Object>of("creation_date", 101.3)), is(false));
        assertThat(matcher.match("ignore", ImmutableMap.<String, Object>of("creation_date", Calendar.getInstance())), is(false));
        assertThat(matcher.match("ignore", ImmutableMap.<String, Object>of("creation_date", new Date())), is(false));
    }

    @Test
    public void error_conditions() {
        AttributeMatcher matcher = new AttributeMatcher("creation_date", new GreaterThanOrEqualToMatcher(100L, DataType.NUMBER), false);
        assertThat(matcher.match("ignore", null), is(false));
        assertThat(matcher.match("ignore", ImmutableMap.<String, Object>of("foo", 101)), is(false));
        assertThat(matcher.match("ignore", ImmutableMap.<String, Object>of("creation_date", "101")), is(false));
    }

    @Test
    public void dates() {
        AttributeMatcher matcher = new AttributeMatcher("creation_date", new GreaterThanOrEqualToMatcher(Calendar.getInstance().getTimeInMillis(), DataType.DATETIME), false);

        Calendar c = Calendar.getInstance();
        c.add(Calendar.YEAR, -1);
        assertThat(matcher.match("ignore", ImmutableMap.<String, Object>of("creation_date", c.getTimeInMillis())), is(false));

        c.add(Calendar.YEAR, 2);
        assertThat(matcher.match("ignore", ImmutableMap.<String, Object>of("creation_date", c.getTimeInMillis())), is(true));
    }

    @Test
    public void between() {
        AttributeMatcher matcher = new AttributeMatcher("creation_date", new BetweenMatcher(10, 12, DataType.NUMBER), false);

        assertThat(matcher.match("ignore", ImmutableMap.<String, Object>of("creation_date", 9)), is(false));
        assertThat(matcher.match("ignore", ImmutableMap.<String, Object>of("creation_date", 10)), is(true));
        assertThat(matcher.match("ignore", ImmutableMap.<String, Object>of("creation_date", 11)), is(true));
        assertThat(matcher.match("ignore", ImmutableMap.<String, Object>of("creation_date", 12)), is(true));
        assertThat(matcher.match("ignore", ImmutableMap.<String, Object>of("creation_date", 13)), is(false));
    }


    @Test
    public void when_no_attribute_we_use_the_key() {
        AttributeMatcher matcher = new AttributeMatcher(null, new WhitelistMatcher(Lists.newArrayList("trial")), false);

        Map<String, Object> nullMap = Maps.newHashMap();
        nullMap.put("planType", null);

        assertThat(matcher.match("trial", ImmutableMap.<String, Object>of("planType", "trial")), is(true));
        assertThat(matcher.match("trial", ImmutableMap.<String, Object>of("planType", "Trial")), is(true));
        assertThat(matcher.match("trial", nullMap), is(true));
        assertThat(matcher.match("trial", ImmutableMap.<String, Object>of("planType", "premium")), is(true));
        assertThat(matcher.match("trial", ImmutableMap.<String, Object>of("planType", 10)), is(true));
        assertThat(matcher.match("trial", Collections.<String, Object>emptyMap()), is(true));
        assertThat(matcher.match("trial", null), is(true));
        assertThat(matcher.match("premium", null), is(false));
    }
}
