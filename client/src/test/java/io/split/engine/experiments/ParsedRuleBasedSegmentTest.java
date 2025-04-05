package io.split.engine.experiments;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.split.client.dtos.ConditionType;
import io.split.client.dtos.MatcherCombiner;
import io.split.engine.matchers.AttributeMatcher;
import io.split.engine.matchers.CombiningMatcher;
import io.split.engine.matchers.UserDefinedSegmentMatcher;

import org.junit.Assert;
import org.junit.Test;

public class ParsedRuleBasedSegmentTest {

    @Test
    public void works() {
        AttributeMatcher segmentMatcher = AttributeMatcher.vanilla(new UserDefinedSegmentMatcher("employees"));
        CombiningMatcher segmentCombiningMatcher = new CombiningMatcher(MatcherCombiner.AND, Lists.newArrayList(segmentMatcher));
        ParsedRuleBasedSegment parsedRuleBasedSegment = new ParsedRuleBasedSegment("another_rule_based_segment",
                Lists.newArrayList(new ParsedCondition(ConditionType.WHITELIST, segmentCombiningMatcher, null, "label")),"user",
                123, Lists.newArrayList("mauro@test.io","gaston@test.io"), Lists.newArrayList("segment1", "segment2"));

        Assert.assertEquals(Sets.newHashSet("employees"), parsedRuleBasedSegment.getSegmentsNames());
        Assert.assertEquals("another_rule_based_segment", parsedRuleBasedSegment.ruleBasedSegment());
        Assert.assertEquals(Lists.newArrayList(new ParsedCondition(ConditionType.WHITELIST, segmentCombiningMatcher, null, "label")),
                parsedRuleBasedSegment.parsedConditions());
        Assert.assertEquals(123, parsedRuleBasedSegment.changeNumber());
    }
}