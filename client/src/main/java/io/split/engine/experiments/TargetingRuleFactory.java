package io.split.engine.experiments;

import io.split.rules.matchers.PrerequisitesMatcher;
import io.split.rules.model.Condition;
import io.split.rules.model.ConditionType;
import io.split.rules.model.Partition;
import io.split.rules.model.Prerequisite;
import io.split.rules.model.TargetingRule;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class TargetingRuleFactory {

    private TargetingRuleFactory() {
        throw new IllegalStateException("Utility class");
    }

    public static TargetingRule buildTargetingRule(
            String feature, int seed, boolean killed, String defaultTreatment,
            List<ParsedCondition> matcherAndSplits, String trafficTypeName, long changeNumber,
            int trafficAllocation, int trafficAllocationSeed, int algo,
            Map<String, String> configurations, HashSet<String> flagSets,
            boolean impressionsDisabled, PrerequisitesMatcher prerequisitesMatcher) {

        List<Condition> conditions = matcherAndSplits == null
                ? Collections.emptyList()
                : matcherAndSplits.stream()
                        .map(TargetingRuleFactory::toTargetingCondition)
                        .collect(Collectors.toList());

        List<Prerequisite> prereqs = prerequisitesMatcher == null
                ? Collections.emptyList()
                : prerequisitesMatcher.getPrerequisites() == null
                        ? Collections.emptyList()
                        : Collections.unmodifiableList(prerequisitesMatcher.getPrerequisites());

        return new TargetingRule(feature, seed, killed, defaultTreatment, conditions, trafficTypeName,
                changeNumber, trafficAllocation, trafficAllocationSeed, algo, configurations,
                flagSets == null ? new HashSet<>() : flagSets, impressionsDisabled, prereqs);
    }

    private static Condition toTargetingCondition(ParsedCondition c) {
        List<Partition> partitions = c.partitions() == null
                ? Collections.emptyList()
                : c.partitions().stream()
                        .map(p -> new Partition(p.treatment, p.size))
                        .collect(Collectors.toList());

        ConditionType condType = c.conditionType() == io.split.client.dtos.ConditionType.ROLLOUT
                ? ConditionType.ROLLOUT
                : ConditionType.WHITELIST;

        return new Condition(condType, c.matcher(), partitions, c.label());
    }
}
