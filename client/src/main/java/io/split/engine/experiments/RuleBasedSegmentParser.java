package io.split.engine.experiments;

import com.google.common.collect.Lists;
import io.split.client.dtos.Condition;
import io.split.client.dtos.Partition;
import io.split.client.dtos.RuleBasedSegment;
import io.split.engine.matchers.CombiningMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static io.split.engine.experiments.ParserUtils.checkUnsupportedMatcherExist;
import static io.split.engine.experiments.ParserUtils.getTemplateCondition;
import static io.split.engine.experiments.ParserUtils.toMatcher;

public final class RuleBasedSegmentParser {

    private static final Logger _log = LoggerFactory.getLogger(RuleBasedSegmentParser.class);

    public RuleBasedSegmentParser() {
    }

    public ParsedRuleBasedSegment parse(RuleBasedSegment ruleBasedSegment) {
        try {
            return parseWithoutExceptionHandling(ruleBasedSegment);
        } catch (Throwable t) {
            _log.error("Could not parse rule based segment: " + ruleBasedSegment, t);
            return null;
        }
    }

    private ParsedRuleBasedSegment parseWithoutExceptionHandling(RuleBasedSegment ruleBasedSegment) {
        List<ParsedCondition> parsedConditionList = Lists.newArrayList();
        for (Condition condition : ruleBasedSegment.conditions) {
            if (checkUnsupportedMatcherExist(condition.matcherGroup.matchers)) {
                _log.error("Unsupported matcher type found for rule based segment: " + ruleBasedSegment.name +
                        " , will revert to default template matcher.");
                parsedConditionList.clear();
                parsedConditionList.add(getTemplateCondition());
                break;
            }
            CombiningMatcher matcher = toMatcher(condition.matcherGroup);
            parsedConditionList.add(new ParsedCondition(condition.conditionType, matcher, partitions, condition.label));
        }

        return new ParsedRuleBasedSegment(
                ruleBasedSegment.name,
                parsedConditionList,
                ruleBasedSegment.trafficTypeName,
                ruleBasedSegment.changeNumber,
                ruleBasedSegment.excluded.keys,
                ruleBasedSegment.excluded.segments);
    }
}