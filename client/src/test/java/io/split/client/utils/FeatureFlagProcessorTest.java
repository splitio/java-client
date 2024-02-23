package io.split.client.utils;

import io.split.client.dtos.Split;
import io.split.client.interceptors.FlagSetsFilter;
import io.split.client.interceptors.FlagSetsFilterImpl;
import io.split.engine.experiments.SplitParser;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static io.split.client.utils.FeatureFlagProcessor.processFeatureFlagChanges;

public class FeatureFlagProcessorTest {

    @Test
    public void testProcessFeatureFlagChanges() {
        SplitParser splitParser = new SplitParser();
        List<Split> featureFlags = new ArrayList<>();

        String definition1 = "{\"trafficTypeName\":\"user\",\"id\":\"d431cdd0-b0be-11ea-8a80-1660ada9ce39\",\"name\":\"mauro_java\",\"trafficAllocation\":100,\"trafficAllocationSeed\":-92391491,\"seed\":-1769377604,\"status\":\"ACTIVE\",\"killed\":false,\"defaultTreatment\":\"off\",\"changeNumber\":1684329854385,\"algo\":2,\"configurations\":{},\"conditions\":[{\"conditionType\":\"WHITELIST\",\"matcherGroup\":{\"combiner\":\"AND\",\"matchers\":[{\"matcherType\":\"WHITELIST\",\"negate\":false,\"whitelistMatcherData\":{\"whitelist\":[\"admin\",\"mauro\",\"nico\"]}}]},\"partitions\":[{\"treatment\":\"off\",\"size\":100}],\"label\":\"whitelisted\"},{\"conditionType\":\"ROLLOUT\",\"matcherGroup\":{\"combiner\":\"AND\",\"matchers\":[{\"keySelector\":{\"trafficType\":\"user\"},\"matcherType\":\"IN_SEGMENT\",\"negate\":false,\"userDefinedSegmentMatcherData\":{\"segmentName\":\"maur-2\"}}]},\"partitions\":[{\"treatment\":\"on\",\"size\":0},{\"treatment\":\"off\",\"size\":100},{\"treatment\":\"V4\",\"size\":0},{\"treatment\":\"v5\",\"size\":0}],\"label\":\"in segment maur-2\"},{\"conditionType\":\"ROLLOUT\",\"matcherGroup\":{\"combiner\":\"AND\",\"matchers\":[{\"keySelector\":{\"trafficType\":\"user\"},\"matcherType\":\"ALL_KEYS\",\"negate\":false}]},\"partitions\":[{\"treatment\":\"on\",\"size\":0},{\"treatment\":\"off\",\"size\":100},{\"treatment\":\"V4\",\"size\":0},{\"treatment\":\"v5\",\"size\":0}],\"label\":\"default rule\"}]}";
        Split featureFlagTest1 = Json.fromJson(definition1, Split.class);

        String definition2 = "{\"trafficTypeName\":\"user\",\"id\":\"d704f220-0567-11ee-80ee-fa3c6460cd13\",\"name\":\"NET_CORE_getTreatmentWithConfigAfterArchive\",\"trafficAllocation\":100,\"trafficAllocationSeed\":179018541,\"seed\":272707374,\"status\":\"ARCHIVED\",\"killed\":false,\"defaultTreatment\":\"V-FGyN\",\"changeNumber\":1686165617166,\"algo\":2,\"configurations\":{\"V-FGyN\":\"{\\\"color\\\":\\\"blue\\\"}\",\"V-YrWB\":\"{\\\"color\\\":\\\"red\\\"}\"},\"conditions\":[{\"conditionType\":\"ROLLOUT\",\"matcherGroup\":{\"combiner\":\"AND\",\"matchers\":[{\"keySelector\":{\"trafficType\":\"user\",\"attribute\":\"test\"},\"matcherType\":\"LESS_THAN_OR_EQUAL_TO\",\"negate\":false,\"unaryNumericMatcherData\":{\"dataType\":\"NUMBER\",\"value\":20}}]},\"partitions\":[{\"treatment\":\"V-FGyN\",\"size\":0},{\"treatment\":\"V-YrWB\",\"size\":100}],\"label\":\"test \\u003c\\u003d 20\"}]}";
        Split featureFlagTest2 = Json.fromJson(definition2, Split.class);

        featureFlags.add(featureFlagTest1);
        featureFlags.add(featureFlagTest2);
        FlagSetsFilter flagSetsFilter = new FlagSetsFilterImpl(new HashSet<>());
        FeatureFlagsToUpdate featureFlagsToUpdate = processFeatureFlagChanges(splitParser, featureFlags, flagSetsFilter);

        Assert.assertEquals(1, featureFlagsToUpdate.toAdd.size());
        Assert.assertEquals(1, featureFlagsToUpdate.toRemove.size());
        Assert.assertEquals(1, featureFlagsToUpdate.segments.size());
    }

    @Test
    public void testProcessFeatureFlagChangesWithSetsToAdd() {
        SplitParser splitParser = new SplitParser();
        List<Split> featureFlags = new ArrayList<>();

        String definition1 = "{\"trafficTypeName\":\"user\",\"id\":\"d431cdd0-b0be-11ea-8a80-1660ada9ce39\",\"name\":\"mauro_java\",\"trafficAllocation\":100,\"trafficAllocationSeed\":-92391491,\"seed\":-1769377604,\"status\":\"ACTIVE\",\"killed\":false,\"defaultTreatment\":\"off\",\"changeNumber\":1684329854385,\"algo\":2,\"configurations\":{},\"sets\":[\"set_1\",\"set_2\"],\"conditions\":[{\"conditionType\":\"WHITELIST\",\"matcherGroup\":{\"combiner\":\"AND\",\"matchers\":[{\"matcherType\":\"WHITELIST\",\"negate\":false,\"whitelistMatcherData\":{\"whitelist\":[\"admin\",\"mauro\",\"nico\"]}}]},\"partitions\":[{\"treatment\":\"off\",\"size\":100}],\"label\":\"whitelisted\"},{\"conditionType\":\"ROLLOUT\",\"matcherGroup\":{\"combiner\":\"AND\",\"matchers\":[{\"keySelector\":{\"trafficType\":\"user\"},\"matcherType\":\"IN_SEGMENT\",\"negate\":false,\"userDefinedSegmentMatcherData\":{\"segmentName\":\"maur-2\"}}]},\"partitions\":[{\"treatment\":\"on\",\"size\":0},{\"treatment\":\"off\",\"size\":100},{\"treatment\":\"V4\",\"size\":0},{\"treatment\":\"v5\",\"size\":0}],\"label\":\"in segment maur-2\"},{\"conditionType\":\"ROLLOUT\",\"matcherGroup\":{\"combiner\":\"AND\",\"matchers\":[{\"keySelector\":{\"trafficType\":\"user\"},\"matcherType\":\"ALL_KEYS\",\"negate\":false}]},\"partitions\":[{\"treatment\":\"on\",\"size\":0},{\"treatment\":\"off\",\"size\":100},{\"treatment\":\"V4\",\"size\":0},{\"treatment\":\"v5\",\"size\":0}],\"label\":\"default rule\"}]}";
        Split featureFlagTest1 = Json.fromJson(definition1, Split.class);

        String definition2 = "{\"trafficTypeName\":\"user\",\"id\":\"d704f220-0567-11ee-80ee-fa3c6460cd13\",\"name\":\"NET_CORE_getTreatmentWithConfigAfterArchive\",\"trafficAllocation\":100,\"trafficAllocationSeed\":179018541,\"seed\":272707374,\"status\":\"ARCHIVED\",\"killed\":false,\"defaultTreatment\":\"V-FGyN\",\"changeNumber\":1686165617166,\"algo\":2,\"configurations\":{\"V-FGyN\":\"{\\\"color\\\":\\\"blue\\\"}\",\"V-YrWB\":\"{\\\"color\\\":\\\"red\\\"}\"},\"conditions\":[{\"conditionType\":\"ROLLOUT\",\"matcherGroup\":{\"combiner\":\"AND\",\"matchers\":[{\"keySelector\":{\"trafficType\":\"user\",\"attribute\":\"test\"},\"matcherType\":\"LESS_THAN_OR_EQUAL_TO\",\"negate\":false,\"unaryNumericMatcherData\":{\"dataType\":\"NUMBER\",\"value\":20}}]},\"partitions\":[{\"treatment\":\"V-FGyN\",\"size\":0},{\"treatment\":\"V-YrWB\",\"size\":100}],\"label\":\"test \\u003c\\u003d 20\"}]}";
        Split featureFlagTest2 = Json.fromJson(definition2, Split.class);

        featureFlags.add(featureFlagTest1);
        featureFlags.add(featureFlagTest2);
        FlagSetsFilter flagSetsFilter = new FlagSetsFilterImpl(new HashSet<>(Arrays.asList("set_1")));
        FeatureFlagsToUpdate featureFlagsToUpdate = processFeatureFlagChanges(splitParser, featureFlags, flagSetsFilter);

        Assert.assertEquals(1, featureFlagsToUpdate.toAdd.size());
        Assert.assertEquals(1, featureFlagsToUpdate.toRemove.size());
        Assert.assertEquals(1, featureFlagsToUpdate.segments.size());
    }

    @Test
    public void testProcessFeatureFlagChangesWithSetsToRemove() {
        SplitParser splitParser = new SplitParser();
        List<Split> featureFlags = new ArrayList<>();

        String definition1 = "{\"trafficTypeName\":\"user\",\"id\":\"d431cdd0-b0be-11ea-8a80-1660ada9ce39\",\"name\":\"mauro_java\",\"trafficAllocation\":100,\"trafficAllocationSeed\":-92391491,\"seed\":-1769377604,\"status\":\"ACTIVE\",\"killed\":false,\"defaultTreatment\":\"off\",\"changeNumber\":1684329854385,\"algo\":2,\"configurations\":{},\"sets\":[\"set_1\",\"set_2\"],\"conditions\":[{\"conditionType\":\"WHITELIST\",\"matcherGroup\":{\"combiner\":\"AND\",\"matchers\":[{\"matcherType\":\"WHITELIST\",\"negate\":false,\"whitelistMatcherData\":{\"whitelist\":[\"admin\",\"mauro\",\"nico\"]}}]},\"partitions\":[{\"treatment\":\"off\",\"size\":100}],\"label\":\"whitelisted\"},{\"conditionType\":\"ROLLOUT\",\"matcherGroup\":{\"combiner\":\"AND\",\"matchers\":[{\"keySelector\":{\"trafficType\":\"user\"},\"matcherType\":\"IN_SEGMENT\",\"negate\":false,\"userDefinedSegmentMatcherData\":{\"segmentName\":\"maur-2\"}}]},\"partitions\":[{\"treatment\":\"on\",\"size\":0},{\"treatment\":\"off\",\"size\":100},{\"treatment\":\"V4\",\"size\":0},{\"treatment\":\"v5\",\"size\":0}],\"label\":\"in segment maur-2\"},{\"conditionType\":\"ROLLOUT\",\"matcherGroup\":{\"combiner\":\"AND\",\"matchers\":[{\"keySelector\":{\"trafficType\":\"user\"},\"matcherType\":\"ALL_KEYS\",\"negate\":false}]},\"partitions\":[{\"treatment\":\"on\",\"size\":0},{\"treatment\":\"off\",\"size\":100},{\"treatment\":\"V4\",\"size\":0},{\"treatment\":\"v5\",\"size\":0}],\"label\":\"default rule\"}]}";
        Split featureFlagTest1 = Json.fromJson(definition1, Split.class);

        String definition2 = "{\"trafficTypeName\":\"user\",\"id\":\"d704f220-0567-11ee-80ee-fa3c6460cd13\",\"name\":\"NET_CORE_getTreatmentWithConfigAfterArchive\",\"trafficAllocation\":100,\"trafficAllocationSeed\":179018541,\"seed\":272707374,\"status\":\"ARCHIVED\",\"killed\":false,\"defaultTreatment\":\"V-FGyN\",\"changeNumber\":1686165617166,\"algo\":2,\"configurations\":{\"V-FGyN\":\"{\\\"color\\\":\\\"blue\\\"}\",\"V-YrWB\":\"{\\\"color\\\":\\\"red\\\"}\"},\"conditions\":[{\"conditionType\":\"ROLLOUT\",\"matcherGroup\":{\"combiner\":\"AND\",\"matchers\":[{\"keySelector\":{\"trafficType\":\"user\",\"attribute\":\"test\"},\"matcherType\":\"LESS_THAN_OR_EQUAL_TO\",\"negate\":false,\"unaryNumericMatcherData\":{\"dataType\":\"NUMBER\",\"value\":20}}]},\"partitions\":[{\"treatment\":\"V-FGyN\",\"size\":0},{\"treatment\":\"V-YrWB\",\"size\":100}],\"label\":\"test \\u003c\\u003d 20\"}]}";
        Split featureFlagTest2 = Json.fromJson(definition2, Split.class);

        featureFlags.add(featureFlagTest1);
        featureFlags.add(featureFlagTest2);
        FlagSetsFilter flagSetsFilter = new FlagSetsFilterImpl(new HashSet<>(Arrays.asList("set_3")));
        FeatureFlagsToUpdate featureFlagsToUpdate = processFeatureFlagChanges(splitParser, featureFlags, flagSetsFilter);

        Assert.assertEquals(0, featureFlagsToUpdate.toAdd.size());
        Assert.assertEquals(2, featureFlagsToUpdate.toRemove.size());
        Assert.assertEquals(0, featureFlagsToUpdate.segments.size());
    }
}