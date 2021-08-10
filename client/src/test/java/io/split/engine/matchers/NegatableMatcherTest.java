package io.split.engine.matchers;

import com.google.common.collect.Lists;
import io.split.storages.SegmentCache;
import io.split.storages.memory.SegmentCacheInMemoryImpl;
import io.split.cache.SegmentCache;
import io.split.cache.SegmentCacheInMemoryImpl;
import io.split.engine.evaluator.EvaluationContext;
import io.split.engine.evaluator.Evaluator;
import io.split.engine.evaluator.EvaluatorImp;
import io.split.engine.matchers.strings.WhitelistMatcher;
import org.junit.Test;
import org.mockito.Mockito;

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

        test(matcher, "foo", false, Mockito.mock(SegmentCache.class));
    }

    @Test
    public void works_segment() {
        SegmentCache segmentCache = new SegmentCacheInMemoryImpl();
        segmentCache.updateSegment("foo", Stream.of("a","b").collect(Collectors.toList()), new ArrayList<>());
        UserDefinedSegmentMatcher delegate = new UserDefinedSegmentMatcher("foo");
        AttributeMatcher.NegatableMatcher matcher = new AttributeMatcher.NegatableMatcher(delegate, true);

        test(matcher, "a", false, segmentCache);
        test(matcher, "b", false, segmentCache);
        test(matcher, "c", true, segmentCache);
    }

    @Test
    public void works_whitelist() {
        WhitelistMatcher delegate = new WhitelistMatcher(Lists.newArrayList("a", "b"));
        AttributeMatcher.NegatableMatcher matcher = new AttributeMatcher.NegatableMatcher(delegate, true);

        test(matcher, "a", false, Mockito.mock(SegmentCache.class));
        test(matcher, "b", false, Mockito.mock(SegmentCache.class));
        test(matcher, "c", true, Mockito.mock(SegmentCache.class));
    }

    private void test(AttributeMatcher.NegatableMatcher negationMatcher, String key, boolean expected, SegmentCache segmentCache) {
        assertThat(negationMatcher.match(key, null, null, new EvaluationContext(Mockito.mock(Evaluator.class), segmentCache)), is(expected));
        assertThat(negationMatcher.delegate().match(key, null, null, new EvaluationContext(Mockito.mock(Evaluator.class), segmentCache)), is(!expected));

    }


}
