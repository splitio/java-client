package io.split.storages.memory;

import com.google.common.collect.Sets;
import io.split.client.dtos.ExcludedSegments;
import io.split.client.dtos.MatcherCombiner;
import io.split.engine.experiments.ParsedRuleBasedSegment;
import io.split.engine.experiments.ParsedCondition;
import io.split.client.dtos.ConditionType;

import io.split.engine.matchers.AttributeMatcher;
import io.split.engine.matchers.CombiningMatcher;
import io.split.engine.matchers.UserDefinedSegmentMatcher;
import io.split.engine.matchers.strings.WhitelistMatcher;
import junit.framework.TestCase;
import org.junit.Test;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;

public class RuleBasedSegmentCacheInMemoryImplTest extends TestCase {

    @Test
    public void testAddAndDeleteSegment(){
        RuleBasedSegmentCacheInMemoryImp ruleBasedSegmentCache = new RuleBasedSegmentCacheInMemoryImp();
        AttributeMatcher whiteListMatcher = AttributeMatcher.vanilla(new WhitelistMatcher(Lists.newArrayList("test_1", "admin")));
        CombiningMatcher whitelistCombiningMatcher = new CombiningMatcher(MatcherCombiner.AND, Lists.newArrayList(whiteListMatcher));
        ParsedRuleBasedSegment parsedRuleBasedSegment = new ParsedRuleBasedSegment("sample_rule_based_segment",
                Lists.newArrayList(new ParsedCondition(ConditionType.WHITELIST, whitelistCombiningMatcher, null, "label")),"user",
                123, Lists.newArrayList("mauro@test.io","gaston@test.io"), Lists.newArrayList());
        ruleBasedSegmentCache.update(Lists.newArrayList(parsedRuleBasedSegment), null, 123);
        assertEquals(123, ruleBasedSegmentCache.getChangeNumber());
        assertEquals(parsedRuleBasedSegment, ruleBasedSegmentCache.get("sample_rule_based_segment"));

        ruleBasedSegmentCache.update(null, Lists.newArrayList("sample_rule_based_segment"), 124);
        assertEquals(124, ruleBasedSegmentCache.getChangeNumber());
        assertEquals(null, ruleBasedSegmentCache.get("sample_rule_based_segment"));
    }

    @Test
    public void testMultipleSegment(){
        List<ExcludedSegments> excludedSegments = new ArrayList<>();
        excludedSegments.add(new ExcludedSegments("standard","segment1"));
        excludedSegments.add(new ExcludedSegments("standard","segment3"));

        RuleBasedSegmentCacheInMemoryImp ruleBasedSegmentCache = new RuleBasedSegmentCacheInMemoryImp();
        AttributeMatcher whiteListMatcher = AttributeMatcher.vanilla(new WhitelistMatcher(Lists.newArrayList("test_1", "admin")));
        CombiningMatcher whitelistCombiningMatcher = new CombiningMatcher(MatcherCombiner.AND, Lists.newArrayList(whiteListMatcher));
        ParsedRuleBasedSegment parsedRuleBasedSegment1 = new ParsedRuleBasedSegment("sample_rule_based_segment",
                Lists.newArrayList(new ParsedCondition(ConditionType.WHITELIST, whitelistCombiningMatcher, null, "label")),"user",
                123, Lists.newArrayList("mauro@test.io","gaston@test.io"), excludedSegments);

        excludedSegments.clear();
        excludedSegments.add(new ExcludedSegments("standard","segment1"));
        excludedSegments.add(new ExcludedSegments("standard","segment2"));
        AttributeMatcher segmentMatcher = AttributeMatcher.vanilla(new UserDefinedSegmentMatcher("employees"));
        CombiningMatcher segmentCombiningMatcher = new CombiningMatcher(MatcherCombiner.AND, Lists.newArrayList(segmentMatcher));
        ParsedRuleBasedSegment parsedRuleBasedSegment2 = new ParsedRuleBasedSegment("another_rule_based_segment",
                Lists.newArrayList(new ParsedCondition(ConditionType.WHITELIST, segmentCombiningMatcher, null, "label")),"user",
                123, Lists.newArrayList("mauro@test.io","gaston@test.io"), excludedSegments);

        ruleBasedSegmentCache.update(Lists.newArrayList(parsedRuleBasedSegment1, parsedRuleBasedSegment2), null, 123);
        assertEquals(Lists.newArrayList("another_rule_based_segment", "sample_rule_based_segment"), ruleBasedSegmentCache.ruleBasedSegmentNames());
        assertEquals(Sets.newHashSet("segment2", "segment1", "employees"), ruleBasedSegmentCache.getSegments());
    }
}