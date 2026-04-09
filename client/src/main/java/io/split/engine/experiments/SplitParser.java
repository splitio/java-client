package io.split.engine.experiments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import io.split.client.dtos.Condition;
import io.split.client.dtos.Partition;
import io.split.client.dtos.Split;
import io.split.rules.matchers.CombiningMatcher;
import io.split.rules.matchers.PrerequisitesMatcher;
import io.split.rules.model.Prerequisite;
import io.split.rules.model.TargetingRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.split.engine.experiments.ParserUtils.checkUnsupportedMatcherExist;
import static io.split.engine.experiments.ParserUtils.getTemplateCondition;
import static io.split.engine.experiments.ParserUtils.toMatcher;

/**
 * Converts io.codigo.dtos.Experiment to io.codigo.engine.splits.ParsedExperiment.
 *
 * @author adil
 */
public final class SplitParser {

    private static final Logger _log = LoggerFactory.getLogger(SplitParser.class);

    public SplitParser() {
    }

    public ParsedSplit parse(Split split) {
        try {
            return parseWithoutExceptionHandling(split);
        } catch (Throwable t) {
            _log.error("Could not parse split: " + split, t);
            return null;
        }
    }

    private ParsedSplit parseWithoutExceptionHandling(Split split) {
        List<ParsedCondition> parsedConditionList = new ArrayList<>();
        List<io.split.rules.model.Condition> targetingConditionList = new ArrayList<>();
        if (Objects.isNull(split.impressionsDisabled)) {
            _log.debug("impressionsDisabled field not detected for Feature flag `" + split.name + "`, setting it to `false`.");
            split.impressionsDisabled = false;
        }
        for (Condition condition : split.conditions) {
            List<Partition> partitions = condition.partitions;
            if (checkUnsupportedMatcherExist(condition.matcherGroup.matchers)) {
                _log.error("Unsupported matcher type found for feature flag: " + split.name + " , will revert to default template matcher.");
                parsedConditionList.clear();
                targetingConditionList.clear();
                parsedConditionList.add(getTemplateCondition());
                io.split.rules.model.Condition templateCondition = toTargetingCondition(getTemplateCondition());
                targetingConditionList.add(templateCondition);
                break;
            }
            CombiningMatcher matcher = toMatcher(condition.matcherGroup);
            parsedConditionList.add(new ParsedCondition(condition.conditionType, matcher, partitions, condition.label));
            targetingConditionList.add(new io.split.rules.model.Condition(
                    toTargetingConditionType(condition.conditionType),
                    matcher,
                    toTargetingPartitions(partitions),
                    condition.label));
        }

        List<Prerequisite> prerequisites = split.prerequisites == null ? Collections.<Prerequisite>emptyList() :
                split.prerequisites.stream()
                        .map(p -> new Prerequisite(p.featureFlagName, p.treatments))
                        .collect(Collectors.toList());

        TargetingRule targetingRule = new TargetingRule(
                split.seed,
                split.killed,
                split.defaultTreatment,
                targetingConditionList,
                split.trafficAllocation,
                split.trafficAllocationSeed,
                split.algo,
                prerequisites);

        return new ParsedSplit(
                split.name,
                split.seed,
                split.killed,
                split.defaultTreatment,
                parsedConditionList,
                split.trafficTypeName,
                split.changeNumber,
                split.trafficAllocation,
                split.trafficAllocationSeed,
                split.algo,
                split.configurations,
                split.sets,
                split.impressionsDisabled,
                new PrerequisitesMatcher(prerequisites),
                targetingRule);
    }

    private static io.split.rules.model.ConditionType toTargetingConditionType(io.split.client.dtos.ConditionType type) {
        return type == io.split.client.dtos.ConditionType.ROLLOUT
                ? io.split.rules.model.ConditionType.ROLLOUT
                : io.split.rules.model.ConditionType.WHITELIST;
    }

    private static List<io.split.rules.model.Partition> toTargetingPartitions(List<Partition> partitions) {
        if (partitions == null) return Collections.emptyList();
        return partitions.stream()
                .map(p -> new io.split.rules.model.Partition(p.treatment, p.size))
                .collect(Collectors.toList());
    }

    private static io.split.rules.model.Condition toTargetingCondition(ParsedCondition parsedCondition) {
        return new io.split.rules.model.Condition(
                toTargetingConditionType(parsedCondition.conditionType()),
                parsedCondition.matcher(),
                toTargetingPartitions(parsedCondition.partitions()),
                parsedCondition.label());
    }
}