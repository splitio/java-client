package io.split.engine.experiments;

import io.split.rules.model.Condition;
import io.split.rules.model.ConditionType;
import io.split.rules.model.Partition;
import io.split.rules.model.Prerequisite;
import io.split.rules.model.TargetingRule;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class TargetingRuleFactory {

    private TargetingRuleFactory() {
        throw new IllegalStateException("Utility class");
    }

    public static TargetingRule buildTargetingRule(
            int seed, boolean killed, String defaultTreatment,
            List<ParsedCondition> matcherAndSplits,
            int trafficAllocation, int trafficAllocationSeed, int algo,
            List<Prerequisite> prerequisites) {

        List<Condition> conditions = matcherAndSplits == null
                ? Collections.emptyList()
                : matcherAndSplits.stream()
                        .map(TargetingRuleFactory::toTargetingCondition)
                        .collect(Collectors.toList());

        return new TargetingRule(seed, killed, defaultTreatment, conditions,
                trafficAllocation, trafficAllocationSeed, algo, prerequisites);
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
