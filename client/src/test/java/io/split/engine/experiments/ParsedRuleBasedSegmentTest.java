package io.split.engine.experiments;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.split.client.dtos.ConditionType;
import io.split.client.dtos.ExcludedSegments;
import io.split.client.dtos.MatcherCombiner;
import io.split.client.dtos.SplitChange;
import io.split.client.utils.Json;
import io.split.client.utils.RuleBasedSegmentsToUpdate;
import io.split.engine.matchers.AttributeMatcher;
import io.split.engine.matchers.CombiningMatcher;
import io.split.engine.matchers.UserDefinedSegmentMatcher;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static io.split.client.utils.RuleBasedSegmentProcessor.processRuleBasedSegmentChanges;

public class ParsedRuleBasedSegmentTest {

    @Test
    public void works() {
        List<ExcludedSegments> excludedSegments = new ArrayList<>();
        excludedSegments.add(new ExcludedSegments("standard","segment1"));
        excludedSegments.add(new ExcludedSegments("standard","segment2"));

        AttributeMatcher segmentMatcher = AttributeMatcher.vanilla(new UserDefinedSegmentMatcher("employees"));
        CombiningMatcher segmentCombiningMatcher = new CombiningMatcher(MatcherCombiner.AND, Lists.newArrayList(segmentMatcher));
        ParsedRuleBasedSegment parsedRuleBasedSegment = new ParsedRuleBasedSegment("another_rule_based_segment",
                Lists.newArrayList(new ParsedCondition(ConditionType.WHITELIST, segmentCombiningMatcher, null, "label")), "user",
                123, Lists.newArrayList("mauro@test.io", "gaston@test.io"), excludedSegments);

        Assert.assertEquals(Sets.newHashSet("employees"), parsedRuleBasedSegment.getSegmentsNames());
        Assert.assertEquals("another_rule_based_segment", parsedRuleBasedSegment.ruleBasedSegment());
        Assert.assertEquals(Lists.newArrayList(new ParsedCondition(ConditionType.WHITELIST, segmentCombiningMatcher, null, "label")),
                parsedRuleBasedSegment.parsedConditions());
        Assert.assertEquals(123, parsedRuleBasedSegment.changeNumber());
    }

    @Test
    public void worksWithoutExcluded() {
        RuleBasedSegmentParser parser = new RuleBasedSegmentParser();
        String load = "{\"ff\":{\"s\":-1,\"t\":-1,\"d\":[]},\"rbs\":{\"s\":-1,\"t\":1457726098069,\"d\":[{ \"changeNumber\": 123, \"trafficTypeName\": \"user\", \"name\": \"some_name\","
                + "\"status\": \"ACTIVE\",\"conditions\": [{\"contitionType\": \"ROLLOUT\","
                + "\"label\": \"some_label\", \"matcherGroup\": { \"matchers\": [{ \"matcherType\": \"ALL_KEYS\", \"negate\": false}],"
                + "\"combiner\": \"AND\"}}]}]}}";
        SplitChange change = Json.fromJson(load, SplitChange.class);
        RuleBasedSegmentsToUpdate toUpdate = processRuleBasedSegmentChanges(parser, change.ruleBasedSegments.d);
        Assert.assertTrue(toUpdate.getToAdd().get(0).excludedKeys().isEmpty());
        Assert.assertTrue(toUpdate.getToAdd().get(0).excludedSegments().isEmpty());

        load = "{\"ff\":{\"s\":-1,\"t\":-1,\"d\":[]},\"rbs\":{\"s\":-1,\"t\":1457726098069,\"d\":[{ \"changeNumber\": 123, \"trafficTypeName\": \"user\", \"name\": \"some_name\","
                + "\"status\": \"ACTIVE\",\"excluded\":{\"segments\":[\"segment1\"]},\"conditions\": [{\"contitionType\": \"ROLLOUT\","
                + "\"label\": \"some_label\", \"matcherGroup\": { \"matchers\": [{ \"matcherType\": \"ALL_KEYS\", \"negate\": false}],"
                + "\"combiner\": \"AND\"}}]}]}}";
        change = Json.fromJson(load, SplitChange.class);
        toUpdate = processRuleBasedSegmentChanges(parser, change.ruleBasedSegments.d);
        Assert.assertTrue(toUpdate.getToAdd().get(0).excludedKeys().isEmpty());

        load = "{\"ff\":{\"s\":-1,\"t\":-1,\"d\":[]},\"rbs\":{\"s\":-1,\"t\":1457726098069,\"d\":[{ \"changeNumber\": 123, \"trafficTypeName\": \"user\", \"name\": \"some_name\","
                + "\"status\": \"ACTIVE\",\"excluded\":{\"segments\":[\"segment1\"], \"keys\":null},\"conditions\": [{\"contitionType\": \"ROLLOUT\","
                + "\"label\": \"some_label\", \"matcherGroup\": { \"matchers\": [{ \"matcherType\": \"ALL_KEYS\", \"negate\": false}],"
                + "\"combiner\": \"AND\"}}]}]}}";
        change = Json.fromJson(load, SplitChange.class);
        toUpdate = processRuleBasedSegmentChanges(parser, change.ruleBasedSegments.d);
        Assert.assertTrue(toUpdate.getToAdd().get(0).excludedKeys().isEmpty());

        load = "{\"ff\":{\"s\":-1,\"t\":-1,\"d\":[]},\"rbs\":{\"s\":-1,\"t\":1457726098069,\"d\":[{ \"changeNumber\": 123, \"trafficTypeName\": \"user\", \"name\": \"some_name\","
                + "\"status\": \"ACTIVE\",\"excluded\":{\"keys\":[\"key1\"]},\"conditions\": [{\"contitionType\": \"ROLLOUT\","
                + "\"label\": \"some_label\", \"matcherGroup\": { \"matchers\": [{ \"matcherType\": \"ALL_KEYS\", \"negate\": false}],"
                + "\"combiner\": \"AND\"}}]}]}}";
        change = Json.fromJson(load, SplitChange.class);
        toUpdate = processRuleBasedSegmentChanges(parser, change.ruleBasedSegments.d);
        Assert.assertTrue(toUpdate.getToAdd().get(0).excludedSegments().isEmpty());

        load = "{\"ff\":{\"s\":-1,\"t\":-1,\"d\":[]},\"rbs\":{\"s\":-1,\"t\":1457726098069,\"d\":[{ \"changeNumber\": 123, \"trafficTypeName\": \"user\", \"name\": \"some_name\","
                + "\"status\": \"ACTIVE\",\"excluded\":{\"segments\":null, \"keys\":[\"key1\"]},\"conditions\": [{\"contitionType\": \"ROLLOUT\","
                + "\"label\": \"some_label\", \"matcherGroup\": { \"matchers\": [{ \"matcherType\": \"ALL_KEYS\", \"negate\": false}],"
                + "\"combiner\": \"AND\"}}]}]}}";
        change = Json.fromJson(load, SplitChange.class);
        toUpdate = processRuleBasedSegmentChanges(parser, change.ruleBasedSegments.d);
        Assert.assertTrue(toUpdate.getToAdd().get(0).excludedSegments().isEmpty());
    }
}