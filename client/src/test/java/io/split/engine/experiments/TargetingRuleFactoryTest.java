package io.split.engine.experiments;

import io.split.client.dtos.ConditionType;
import io.split.client.dtos.Partition;
import io.split.rules.matchers.AllKeysMatcher;
import io.split.rules.matchers.AttributeMatcher;
import io.split.rules.matchers.CombiningMatcher;
import io.split.rules.model.Condition;
import io.split.rules.model.TargetingRule;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class TargetingRuleFactoryTest {

    private Map<String, String> _configurations;
    private HashSet<String> _flagSets;

    @Before
    public void setUp() {
        _configurations = new HashMap<>();
        _configurations.put("on", "{\"color\": \"blue\"}");
        _flagSets = new HashSet<>(Arrays.asList("set1", "set2"));
    }

    @Test
    public void testBuildTargetingRule_withNullConditions_returnsEmptyList() {
        TargetingRule rule = TargetingRuleFactory.buildTargetingRule(
                "feature1",
                12345,
                false,
                "control",
                null,
                "user_type",
                999L,
                100,
                456,
                1,
                _configurations,
                _flagSets,
                false,
                null
        );

        assertNotNull(rule);
        assertEquals("feature1", rule.name());
        assertTrue(rule.conditions().isEmpty());
    }

    @Test
    public void testBuildTargetingRule_withValidConditions_mapsCorrectly() {
        CombiningMatcher matcher = new CombiningMatcher(CombiningMatcher.Combiner.AND,
                new ArrayList<>(Arrays.asList(new AttributeMatcher(null, new AllKeysMatcher(), false))));

        Partition dtoPartition = new Partition();
        dtoPartition.treatment = "on";
        dtoPartition.size = 100;

        ParsedCondition parsedCondition = new ParsedCondition(
                ConditionType.ROLLOUT,
                matcher,
                Arrays.asList(dtoPartition),
                "label1"
        );

        TargetingRule rule = TargetingRuleFactory.buildTargetingRule(
                "feature1",
                12345,
                false,
                "control",
                Arrays.asList(parsedCondition),
                "user_type",
                999L,
                100,
                456,
                1,
                _configurations,
                _flagSets,
                false,
                null
        );

        assertNotNull(rule);
        assertEquals("feature1", rule.name());
        assertEquals(1, rule.conditions().size());

        Condition condition = rule.conditions().get(0);
        assertEquals(io.split.rules.model.ConditionType.ROLLOUT, condition.conditionType());
        assertEquals("label1", condition.label());
        assertEquals(1, condition.partitions().size());

        io.split.rules.model.Partition partition = condition.partitions().get(0);
        assertEquals("on", partition.treatment);
        assertEquals(100, partition.size);
    }

    @Test
    public void testBuildTargetingRule_withNullPrerequisites_returnsEmptyPrerequisiteList() {
        TargetingRule rule = TargetingRuleFactory.buildTargetingRule(
                "feature1",
                12345,
                false,
                "control",
                new ArrayList<>(),
                "user_type",
                999L,
                100,
                456,
                1,
                _configurations,
                _flagSets,
                false,
                null
        );

        assertNotNull(rule);
        assertTrue(rule.prerequisites().isEmpty());
    }

    @Test
    public void testBuildTargetingRule_withNullFlagSets_createsEmptySet() {
        TargetingRule rule = TargetingRuleFactory.buildTargetingRule(
                "feature1",
                12345,
                false,
                "control",
                new ArrayList<>(),
                "user_type",
                999L,
                100,
                456,
                1,
                _configurations,
                null,
                false,
                null
        );

        assertNotNull(rule);
        assertNotNull(rule.flagSets());
        assertTrue(rule.flagSets().isEmpty());
    }

    @Test
    public void testBuildTargetingRule_withFlagSets_preservesSet() {
        TargetingRule rule = TargetingRuleFactory.buildTargetingRule(
                "feature1",
                12345,
                false,
                "control",
                new ArrayList<>(),
                "user_type",
                999L,
                100,
                456,
                1,
                _configurations,
                _flagSets,
                false,
                null
        );

        assertNotNull(rule);
        assertEquals(_flagSets, rule.flagSets());
    }

    @Test
    public void testBuildTargetingRule_withRolloutCondition_mapsConditionTypeCorrectly() {
        CombiningMatcher matcher = new CombiningMatcher(CombiningMatcher.Combiner.AND,
                new ArrayList<>(Arrays.asList(new AttributeMatcher(null, new AllKeysMatcher(), false))));

        Partition dtoPartition = new Partition();
        dtoPartition.treatment = "on";
        dtoPartition.size = 50;

        ParsedCondition rolloutCondition = new ParsedCondition(
                ConditionType.ROLLOUT,
                matcher,
                Arrays.asList(dtoPartition),
                "rollout_label"
        );

        TargetingRule rule = TargetingRuleFactory.buildTargetingRule(
                "feature1", 12345, false, "control",
                Arrays.asList(rolloutCondition),
                "user_type", 999L, 100, 456, 1,
                _configurations, _flagSets, false, null
        );

        Condition condition = rule.conditions().get(0);
        assertEquals(io.split.rules.model.ConditionType.ROLLOUT, condition.conditionType());
    }

    @Test
    public void testBuildTargetingRule_withWhitelistCondition_mapsConditionTypeCorrectly() {
        CombiningMatcher matcher = new CombiningMatcher(CombiningMatcher.Combiner.AND,
                new ArrayList<>(Arrays.asList(new AttributeMatcher(null, new AllKeysMatcher(), false))));

        Partition dtoPartition = new Partition();
        dtoPartition.treatment = "special";
        dtoPartition.size = 100;

        ParsedCondition whitelistCondition = new ParsedCondition(
                ConditionType.WHITELIST,
                matcher,
                Arrays.asList(dtoPartition),
                "whitelist_label"
        );

        TargetingRule rule = TargetingRuleFactory.buildTargetingRule(
                "feature1", 12345, false, "control",
                Arrays.asList(whitelistCondition),
                "user_type", 999L, 100, 456, 1,
                _configurations, _flagSets, false, null
        );

        Condition condition = rule.conditions().get(0);
        assertEquals(io.split.rules.model.ConditionType.WHITELIST, condition.conditionType());
    }

    @Test
    public void testBuildTargetingRule_withMultiplePartitions_mapsAllPartitions() {
        CombiningMatcher matcher = new CombiningMatcher(CombiningMatcher.Combiner.AND,
                new ArrayList<>(Arrays.asList(new AttributeMatcher(null, new AllKeysMatcher(), false))));

        Partition partition1 = new Partition();
        partition1.treatment = "on";
        partition1.size = 50;

        Partition partition2 = new Partition();
        partition2.treatment = "off";
        partition2.size = 50;

        ParsedCondition parsedCondition = new ParsedCondition(
                ConditionType.ROLLOUT,
                matcher,
                Arrays.asList(partition1, partition2),
                "multi_partition_label"
        );

        TargetingRule rule = TargetingRuleFactory.buildTargetingRule(
                "feature1", 12345, false, "control",
                Arrays.asList(parsedCondition),
                "user_type", 999L, 100, 456, 1,
                _configurations, _flagSets, false, null
        );

        Condition condition = rule.conditions().get(0);
        assertEquals(2, condition.partitions().size());
        assertEquals("on", condition.partitions().get(0).treatment);
        assertEquals(50, condition.partitions().get(0).size);
        assertEquals("off", condition.partitions().get(1).treatment);
        assertEquals(50, condition.partitions().get(1).size);
    }

    @Test
    public void testBuildTargetingRule_withNullPartitions_returnsEmptyPartitionList() {
        CombiningMatcher matcher = new CombiningMatcher(CombiningMatcher.Combiner.AND,
                new ArrayList<>(Arrays.asList(new AttributeMatcher(null, new AllKeysMatcher(), false))));

        ParsedCondition parsedCondition = new ParsedCondition(
                ConditionType.ROLLOUT,
                matcher,
                null,
                "null_partitions_label"
        );

        TargetingRule rule = TargetingRuleFactory.buildTargetingRule(
                "feature1", 12345, false, "control",
                Arrays.asList(parsedCondition),
                "user_type", 999L, 100, 456, 1,
                _configurations, _flagSets, false, null
        );

        Condition condition = rule.conditions().get(0);
        assertTrue(condition.partitions().isEmpty());
    }

    @Test
    public void testBuildTargetingRule_preservesAllNonMappedFields() {
        TargetingRule rule = TargetingRuleFactory.buildTargetingRule(
                "my_feature",
                98765,
                true,
                "killed",
                new ArrayList<>(),
                "account",
                555L,
                75,
                321,
                2,
                _configurations,
                _flagSets,
                true,
                null
        );

        assertEquals("my_feature", rule.name());
        assertEquals(98765, rule.seed());
        assertTrue(rule.killed());
        assertEquals("killed", rule.defaultTreatment());
        assertEquals("account", rule.trafficTypeName());
        assertEquals(555L, rule.changeNumber());
        assertEquals(75, rule.trafficAllocation());
        assertEquals(321, rule.trafficAllocationSeed());
        assertEquals(2, rule.algo());
        assertEquals(_configurations, rule.configurations());
        assertTrue(rule.impressionsDisabled());
    }
}
