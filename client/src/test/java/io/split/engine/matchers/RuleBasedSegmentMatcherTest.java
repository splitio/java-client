package io.split.engine.matchers;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.split.client.dtos.ConditionType;
import io.split.client.dtos.MatcherCombiner;
import io.split.engine.evaluator.EvaluationContext;
import io.split.engine.evaluator.Evaluator;
import io.split.engine.experiments.ParsedCondition;
import io.split.engine.experiments.ParsedRuleBasedSegment;
import io.split.engine.matchers.strings.WhitelistMatcher;
import io.split.storages.RuleBasedSegmentCache;
import io.split.storages.RuleBasedSegmentCacheConsumer;
import io.split.storages.SegmentCache;
import io.split.storages.memory.RuleBasedSegmentCacheInMemoryImp;
import io.split.storages.memory.SegmentCacheInMemoryImpl;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Set;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class RuleBasedSegmentMatcherTest {
    @Test
    public void works() {
        Evaluator evaluator = Mockito.mock(Evaluator.class);
        SegmentCache segmentCache = new SegmentCacheInMemoryImpl();
        RuleBasedSegmentCache ruleBasedSegmentCache = new RuleBasedSegmentCacheInMemoryImp();
        EvaluationContext evaluationContext = new EvaluationContext(evaluator, segmentCache, ruleBasedSegmentCache);
        AttributeMatcher whiteListMatcher = AttributeMatcher.vanilla(new WhitelistMatcher(Lists.newArrayList("test_1", "admin")));
        CombiningMatcher whitelistCombiningMatcher = new CombiningMatcher(MatcherCombiner.AND, Lists.newArrayList(whiteListMatcher));

        AttributeMatcher ruleBasedSegmentMatcher = AttributeMatcher.vanilla(new RuleBasedSegmentMatcher("sample_rule_based_segment"));
        CombiningMatcher ruleBasedSegmentCombinerMatcher = new CombiningMatcher(MatcherCombiner.AND, Lists.newArrayList(ruleBasedSegmentMatcher));
        ParsedCondition ruleBasedSegmentCondition = new ParsedCondition(ConditionType.ROLLOUT, ruleBasedSegmentCombinerMatcher, null, "test rbs rule");
        ParsedRuleBasedSegment parsedRuleBasedSegment = new ParsedRuleBasedSegment("sample_rule_based_segment",
                Lists.newArrayList(new ParsedCondition(ConditionType.WHITELIST, whitelistCombiningMatcher, null, "whitelist label")),"user",
                123, Lists.newArrayList("mauro@test.io","gaston@test.io"), Lists.newArrayList());
        ruleBasedSegmentCache.update(Lists.newArrayList(parsedRuleBasedSegment), null, 123);

        RuleBasedSegmentMatcher matcher = new RuleBasedSegmentMatcher("sample_rule_based_segment");

        assertThat(matcher.match("mauro@test.io", null, null, evaluationContext), is(false));
        assertThat(matcher.match("admin", null, null, evaluationContext), is(true));

        assertThat(matcher.match("foo", null, null, evaluationContext), is(false));
        assertThat(matcher.match(null, null, null, evaluationContext), is(false));

    }

}
