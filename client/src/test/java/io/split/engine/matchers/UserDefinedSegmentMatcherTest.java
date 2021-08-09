package io.split.engine.matchers;

import com.google.common.collect.Sets;
import io.split.cache.SegmentCache;
import io.split.cache.SegmentCacheInMemoryImpl;
import io.split.engine.evaluator.EvaluationContext;
import io.split.engine.evaluator.Evaluator;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Tests for UserDefinedSegmentMatcher
 *
 * @author adil
 */
public class UserDefinedSegmentMatcherTest {
    @Test
    public void works() {
        Set<String> keys = Sets.newHashSet("a", "b");
        Evaluator evaluator = Mockito.mock(Evaluator.class);
        SegmentCache segmentCache = new SegmentCacheInMemoryImpl();
        EvaluationContext evaluationContext = new EvaluationContext(evaluator, segmentCache);
        segmentCache.updateSegment("foo", Stream.of("a","b").collect(Collectors.toList()), new ArrayList<>());
        UserDefinedSegmentMatcher matcher = new UserDefinedSegmentMatcher("foo");

        for (String key : keys) {
            assertThat(matcher.match(key, null, null, evaluationContext), is(true));
        }

        assertThat(matcher.match("foo", null, null, evaluationContext), is(false));
        assertThat(matcher.match(null, null, null, evaluationContext), is(false));

    }

}
