package io.split.engine.matchers;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.split.cache.SegmentCache;
import io.split.cache.SegmentCacheInMemoryImpl;
import io.split.engine.matchers.strings.WhitelistMatcher;
import io.split.engine.segments.StaticSegment;
import org.junit.Test;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Tests for NegatableMatcher.
 *
 * @author adil
 */
public class NegatableMatcherTest {

    @Test
    public void works_all_keys() {
        AllKeysMatcher delegate = new AllKeysMatcher();
        AttributeMatcher.NegatableMatcher matcher = new AttributeMatcher.NegatableMatcher(delegate, true);

        test(matcher, "foo", false);
    }

    @Test
    public void works_segment() {
        SegmentCache segmentCache = new SegmentCacheInMemoryImpl();
        segmentCache.updateSegment("foo", Stream.of("a","b").collect(Collectors.toList()), new ArrayList<>());
        UserDefinedSegmentMatcher delegate = new UserDefinedSegmentMatcher(segmentCache, "foo");
        AttributeMatcher.NegatableMatcher matcher = new AttributeMatcher.NegatableMatcher(delegate, true);

        test(matcher, "a", false);
        test(matcher, "b", false);
        test(matcher, "c", true);
    }

    @Test
    public void works_whitelist() {
        WhitelistMatcher delegate = new WhitelistMatcher(Lists.newArrayList("a", "b"));
        AttributeMatcher.NegatableMatcher matcher = new AttributeMatcher.NegatableMatcher(delegate, true);

        test(matcher, "a", false);
        test(matcher, "b", false);
        test(matcher, "c", true);
    }

    private void test(AttributeMatcher.NegatableMatcher negationMatcher, String key, boolean expected) {
        assertThat(negationMatcher.match(key, null, null, null), is(expected));
        assertThat(negationMatcher.delegate().match(key, null, null, null), is(!expected));

    }


}
