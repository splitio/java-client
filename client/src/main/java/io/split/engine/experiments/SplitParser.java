package io.split.engine.experiments;

import com.google.common.collect.Lists;
import io.split.client.dtos.Condition;
import io.split.client.dtos.Matcher;
import io.split.client.dtos.MatcherGroup;
import io.split.client.dtos.Partition;
import io.split.client.dtos.Split;
import io.split.client.dtos.ConditionType;
import io.split.client.dtos.MatcherType;
import io.split.engine.evaluator.Labels;
import io.split.engine.matchers.AllKeysMatcher;
import io.split.engine.matchers.AttributeMatcher;
import io.split.engine.matchers.BetweenMatcher;
import io.split.engine.matchers.BooleanMatcher;
import io.split.engine.matchers.CombiningMatcher;
import io.split.engine.matchers.DependencyMatcher;
import io.split.engine.matchers.EqualToMatcher;
import io.split.engine.matchers.GreaterThanOrEqualToMatcher;
import io.split.engine.matchers.LessThanOrEqualToMatcher;
import io.split.engine.matchers.UserDefinedSegmentMatcher;
import io.split.engine.matchers.collections.ContainsAllOfSetMatcher;
import io.split.engine.matchers.collections.ContainsAnyOfSetMatcher;
import io.split.engine.matchers.collections.EqualToSetMatcher;
import io.split.engine.matchers.collections.PartOfSetMatcher;
import io.split.engine.matchers.strings.ContainsAnyOfMatcher;
import io.split.engine.matchers.strings.EndsWithAnyOfMatcher;
import io.split.engine.matchers.strings.RegularExpressionMatcher;
import io.split.engine.matchers.strings.StartsWithAnyOfMatcher;
import io.split.engine.matchers.strings.WhitelistMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

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

        return new ParsedSplit(split.name, split.seed, split.killed, split.defaultTreatment, parsedConditionList, split.trafficTypeName,
                split.changeNumber, split.trafficAllocation, split.trafficAllocationSeed, split.algo, split.configurations, split.sets);
    }

    private boolean checkUnsupportedMatcherExist(List<io.split.client.dtos.Matcher> matchers) {
        MatcherType typeCheck = null;
        for (io.split.client.dtos.Matcher matcher : matchers) {
            typeCheck = null;
            try {
                typeCheck = matcher.matcherType;
            } catch (NullPointerException e) {
                // If the exception is caught, it means unsupported matcher
                break;
            }
        }
        if (typeCheck != null)  return false;
        return true;
    }

    private ParsedCondition getTemplateCondition() {
        List<Partition> templatePartitions = Lists.newArrayList();
        Partition partition = new Partition();
        partition.treatment = "control";
        partition.size = 100;
        templatePartitions.add(partition);
        return new ParsedCondition(
                ConditionType.ROLLOUT,
                CombiningMatcher.of(new AllKeysMatcher()),
                templatePartitions,
                Labels.UNSUPPORTED_MATCHER);
    }

    private CombiningMatcher toMatcher(MatcherGroup matcherGroup) {
        List<io.split.client.dtos.Matcher> matchers = matcherGroup.matchers;
        checkArgument(!matchers.isEmpty());

        List<AttributeMatcher> toCombine = Lists.newArrayList();

        for (io.split.client.dtos.Matcher matcher : matchers) {
            toCombine.add(toMatcher(matcher));
        }

        return new CombiningMatcher(matcherGroup.combiner, toCombine);
    }


    private AttributeMatcher toMatcher(Matcher matcher) {
        io.split.engine.matchers.Matcher delegate = null;
        switch (matcher.matcherType) {
            case ALL_KEYS:
                delegate = new AllKeysMatcher();
                break;
            case IN_SEGMENT:
                checkNotNull(matcher.userDefinedSegmentMatcherData);
                String segmentName = matcher.userDefinedSegmentMatcherData.segmentName;
                delegate = new UserDefinedSegmentMatcher(segmentName);
                break;
            case WHITELIST:
                checkNotNull(matcher.whitelistMatcherData);
                delegate = new WhitelistMatcher(matcher.whitelistMatcherData.whitelist);
                break;
            case EQUAL_TO:
                checkNotNull(matcher.unaryNumericMatcherData);
                delegate = new EqualToMatcher(matcher.unaryNumericMatcherData.value, matcher.unaryNumericMatcherData.dataType);
                break;
            case GREATER_THAN_OR_EQUAL_TO:
                checkNotNull(matcher.unaryNumericMatcherData);
                delegate = new GreaterThanOrEqualToMatcher(matcher.unaryNumericMatcherData.value, matcher.unaryNumericMatcherData.dataType);
                break;
            case LESS_THAN_OR_EQUAL_TO:
                checkNotNull(matcher.unaryNumericMatcherData);
                delegate = new LessThanOrEqualToMatcher(matcher.unaryNumericMatcherData.value, matcher.unaryNumericMatcherData.dataType);
                break;
            case BETWEEN:
                checkNotNull(matcher.betweenMatcherData);
                delegate = new BetweenMatcher(matcher.betweenMatcherData.start, matcher.betweenMatcherData.end, matcher.betweenMatcherData.dataType);
                break;
            case EQUAL_TO_SET:
                checkNotNull(matcher.whitelistMatcherData);
                delegate = new EqualToSetMatcher(matcher.whitelistMatcherData.whitelist);
                break;
            case PART_OF_SET:
                checkNotNull(matcher.whitelistMatcherData);
                delegate = new PartOfSetMatcher(matcher.whitelistMatcherData.whitelist);
                break;
            case CONTAINS_ALL_OF_SET:
                checkNotNull(matcher.whitelistMatcherData);
                delegate = new ContainsAllOfSetMatcher(matcher.whitelistMatcherData.whitelist);
                break;
            case CONTAINS_ANY_OF_SET:
                checkNotNull(matcher.whitelistMatcherData);
                delegate = new ContainsAnyOfSetMatcher(matcher.whitelistMatcherData.whitelist);
                break;
            case STARTS_WITH:
                checkNotNull(matcher.whitelistMatcherData);
                delegate = new StartsWithAnyOfMatcher(matcher.whitelistMatcherData.whitelist);
                break;
            case ENDS_WITH:
                checkNotNull(matcher.whitelistMatcherData);
                delegate = new EndsWithAnyOfMatcher(matcher.whitelistMatcherData.whitelist);
                break;
            case CONTAINS_STRING:
                checkNotNull(matcher.whitelistMatcherData);
                delegate = new ContainsAnyOfMatcher(matcher.whitelistMatcherData.whitelist);
                break;
            case MATCHES_STRING:
                checkNotNull(matcher.stringMatcherData);
                delegate = new RegularExpressionMatcher(matcher.stringMatcherData);
                break;
            case IN_SPLIT_TREATMENT:
                checkNotNull(matcher.dependencyMatcherData,
                        "MatcherType is " + matcher.matcherType
                                + ". matcher.dependencyMatcherData() MUST NOT BE null");
                delegate = new DependencyMatcher(matcher.dependencyMatcherData.split, matcher.dependencyMatcherData.treatments);
                break;
            case EQUAL_TO_BOOLEAN:
                checkNotNull(matcher.booleanMatcherData,
                        "MatcherType is " + matcher.matcherType
                                + ". matcher.booleanMatcherData() MUST NOT BE null");
                delegate = new BooleanMatcher(matcher.booleanMatcherData);
                break;
            default:
                throw new IllegalArgumentException("Unknown matcher type: " + matcher.matcherType);
        }

        checkNotNull(delegate, "We were not able to create a matcher for: " + matcher.matcherType);

        String attribute = null;
        if (matcher.keySelector != null && matcher.keySelector.attribute != null) {
            attribute = matcher.keySelector.attribute;
        }

        boolean negate = matcher.negate;


        return new AttributeMatcher(attribute, delegate, negate);
    }
}