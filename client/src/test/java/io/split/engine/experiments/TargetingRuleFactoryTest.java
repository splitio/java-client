package io.split.engine.experiments;

import io.split.client.dtos.ConditionType;
import io.split.client.dtos.Partition;
import io.split.rules.matchers.AllKeysMatcher;
import io.split.rules.matchers.AttributeMatcher;
import io.split.rules.matchers.CombiningMatcher;
import io.split.rules.model.Condition;
import io.split.rules.model.TargetingRule;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class TargetingRuleFactoryTest {

    @Test
    public void testBuildTargetingRule_withNullConditions_returnsEmptyList() {
        TargetingRule rule = TargetingRuleFactory.buildTargetingRule(
                12345, false, "control",
                null,
                100, 456, 1,
                null
        );

        assertNotNull(rule);
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
                12345, false, "control",
                Arrays.asList(parsedCondition),
                100, 456, 1,
                null
        );

        assertNotNull(rule);
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
                12345, false, "control",
                new ArrayList<ParsedCondition>(),
                100, 456, 1,
                null
        );

        assertNotNull(rule);
        assertTrue(rule.prerequisites().isEmpty());
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
                12345, false, "control",
                Arrays.asList(rolloutCondition),
                100, 456, 1,
                null
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
                12345, false, "control",
                Arrays.asList(whitelistCondition),
                100, 456, 1,
                null
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
                12345, false, "control",
                Arrays.asList(parsedCondition),
                100, 456, 1,
                null
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
                12345, false, "control",
                Arrays.asList(parsedCondition),
                100, 456, 1,
                null
        );

        Condition condition = rule.conditions().get(0);
        assertTrue(condition.partitions().isEmpty());
    }

    @Test
    public void testBuildTargetingRule_preservesEvaluationFields() {
        TargetingRule rule = TargetingRuleFactory.buildTargetingRule(
                98765, true, "killed",
                new ArrayList<ParsedCondition>(),
                75, 321, 2,
                null
        );

        assertEquals(98765, rule.seed());
        assertTrue(rule.killed());
        assertEquals("killed", rule.defaultTreatment());
        assertEquals(75, rule.trafficAllocation());
        assertEquals(321, rule.trafficAllocationSeed());
        assertEquals(2, rule.algo());
        assertTrue(rule.prerequisites().isEmpty());
    }

    @Test
    public void testBuildTargetingRule_withPrerequisites_preservesList() {
        List<io.split.rules.model.Prerequisite> prereqs = Collections.singletonList(
                new io.split.rules.model.Prerequisite("flag1", Collections.singletonList("on"))
        );

        TargetingRule rule = TargetingRuleFactory.buildTargetingRule(
                12345, false, "control",
                new ArrayList<ParsedCondition>(),
                100, 456, 1,
                prereqs
        );

        assertEquals(1, rule.prerequisites().size());
        assertEquals("flag1", rule.prerequisites().get(0).featureFlagName());
    }
}
