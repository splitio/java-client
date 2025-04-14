package io.split.engine.experiments;

import com.google.common.collect.Lists;

import io.split.client.dtos.Condition;
import io.split.client.dtos.Partition;
import io.split.client.dtos.Split;
import io.split.engine.matchers.CombiningMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

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
        List<ParsedCondition> parsedConditionList = Lists.newArrayList();
        if (Objects.isNull(split.impressionsDisabled)) {
            _log.debug("impressionsDisabled field not detected for Feature flag `" + split.name + "`, setting it to `false`.");
            split.impressionsDisabled = false;
        }
        for (Condition condition : split.conditions) {
            List<Partition> partitions = condition.partitions;
            if (checkUnsupportedMatcherExist(condition.matcherGroup.matchers)) {
                _log.error("Unsupported matcher type found for feature flag: " + split.name + " , will revert to default template matcher.");
                parsedConditionList.clear();
                parsedConditionList.add(getTemplateCondition());
                break;
            }
            CombiningMatcher matcher = toMatcher(condition.matcherGroup);
            parsedConditionList.add(new ParsedCondition(condition.conditionType, matcher, partitions, condition.label));
        }

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
                split.impressionsDisabled);
    }
}