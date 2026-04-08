package io.split.engine.experiments;

import io.split.client.dtos.DataType;
import io.split.client.dtos.MatcherCombiner;
import io.split.client.dtos.MatcherType;
import io.split.client.dtos.Partition;
import io.split.client.dtos.MatcherGroup;
import io.split.client.dtos.ConditionType;
import io.split.client.dtos.Matcher;
import io.split.engine.evaluator.Labels;
import io.split.rules.matchers.CombiningMatcher;
import io.split.rules.matchers.AllKeysMatcher;
import io.split.rules.matchers.AttributeMatcher;
import io.split.rules.matchers.UserDefinedSegmentMatcher;
import io.split.rules.matchers.EqualToMatcher;
import io.split.rules.matchers.GreaterThanOrEqualToMatcher;
import io.split.rules.matchers.LessThanOrEqualToMatcher;
import io.split.rules.matchers.BetweenMatcher;
import io.split.rules.matchers.DependencyMatcher;
import io.split.rules.matchers.BooleanMatcher;
import io.split.rules.matchers.EqualToSemverMatcher;
import io.split.rules.matchers.GreaterThanOrEqualToSemverMatcher;
import io.split.rules.matchers.LessThanOrEqualToSemverMatcher;
import io.split.rules.matchers.InListSemverMatcher;
import io.split.rules.matchers.BetweenSemverMatcher;
import io.split.rules.matchers.RuleBasedSegmentMatcher;
import io.split.rules.matchers.collections.ContainsAllOfSetMatcher;
import io.split.rules.matchers.collections.ContainsAnyOfSetMatcher;
import io.split.rules.matchers.collections.EqualToSetMatcher;
import io.split.rules.matchers.collections.PartOfSetMatcher;
import io.split.rules.matchers.WhitelistMatcher;
import io.split.rules.matchers.strings.StartsWithAnyOfMatcher;
import io.split.rules.matchers.strings.EndsWithAnyOfMatcher;
import io.split.rules.matchers.strings.ContainsAnyOfMatcher;
import io.split.rules.matchers.strings.RegularExpressionMatcher;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public final class ParserUtils {

    private ParserUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static boolean checkUnsupportedMatcherExist(List<Matcher> matchers) {
        MatcherType typeCheck = null;
        for (Matcher matcher : matchers) {
            typeCheck = null;
            try {
                typeCheck = matcher.matcherType;
            } catch (NullPointerException e) {
                // If the exception is caught, it means unsupported matcher
                break;
            }
        }
        return (typeCheck == null);
    }

    public static ParsedCondition getTemplateCondition() {
        List<Partition> templatePartitions = new ArrayList<>();
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

    public static CombiningMatcher toMatcher(MatcherGroup matcherGroup) {
        List<Matcher> matchers = matcherGroup.matchers;
        if (matchers.isEmpty()) throw new IllegalArgumentException();

        List<AttributeMatcher> toCombine = new ArrayList<>();

        for (Matcher matcher : matchers) {
            toCombine.add(toMatcher(matcher));
        }

        return new CombiningMatcher(toCombiner(matcherGroup.combiner), toCombine);
    }


    private static io.split.rules.model.DataType toRulesDataType(io.split.client.dtos.DataType dt) {
        return io.split.rules.model.DataType.valueOf(dt.name());
    }

    private static CombiningMatcher.Combiner toCombiner(MatcherCombiner combiner) {
        return CombiningMatcher.Combiner.valueOf(combiner.name());
    }

    public static AttributeMatcher toMatcher(Matcher matcher) {
        io.split.rules.matchers.Matcher delegate = null;
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
                delegate = new EqualToMatcher(matcher.unaryNumericMatcherData.value, toRulesDataType(matcher.unaryNumericMatcherData.dataType));
                break;
            case GREATER_THAN_OR_EQUAL_TO:
                checkNotNull(matcher.unaryNumericMatcherData);
                delegate = new GreaterThanOrEqualToMatcher(
                        matcher.unaryNumericMatcherData.value, toRulesDataType(matcher.unaryNumericMatcherData.dataType));
                break;
            case LESS_THAN_OR_EQUAL_TO:
                checkNotNull(matcher.unaryNumericMatcherData);
                delegate = new LessThanOrEqualToMatcher(
                        matcher.unaryNumericMatcherData.value, toRulesDataType(matcher.unaryNumericMatcherData.dataType));
                break;
            case BETWEEN:
                checkNotNull(matcher.betweenMatcherData);
                delegate = new BetweenMatcher(matcher.betweenMatcherData.start,
                        matcher.betweenMatcherData.end, toRulesDataType(matcher.betweenMatcherData.dataType));
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
                delegate = new StartsWithAnyOfMatcher(matcher.whitelistMatcherData.whitelist);
                break;
            case ENDS_WITH:
                delegate = new EndsWithAnyOfMatcher(matcher.whitelistMatcherData.whitelist);
                break;
            case CONTAINS_STRING:
                delegate = new ContainsAnyOfMatcher(matcher.whitelistMatcherData.whitelist);
                break;
            case MATCHES_STRING:
                delegate = new RegularExpressionMatcher(matcher.stringMatcherData);
                break;
            case IN_SPLIT_TREATMENT:
                if (matcher.dependencyMatcherData == null) throw new NullPointerException(
                        "MatcherType is " + matcher.matcherType + ". matcher.dependencyMatcherData() MUST NOT BE null");
                delegate = new DependencyMatcher(matcher.dependencyMatcherData.split, matcher.dependencyMatcherData.treatments);
                break;
            case EQUAL_TO_BOOLEAN:
                if (matcher.booleanMatcherData == null) throw new NullPointerException(
                        "MatcherType is " + matcher.matcherType + ". matcher.booleanMatcherData() MUST NOT BE null");
                delegate = new BooleanMatcher(matcher.booleanMatcherData);
                break;
            case EQUAL_TO_SEMVER:
                delegate = new EqualToSemverMatcher(matcher.stringMatcherData);
                break;
            case GREATER_THAN_OR_EQUAL_TO_SEMVER:
                delegate = new GreaterThanOrEqualToSemverMatcher(matcher.stringMatcherData);
                break;
            case LESS_THAN_OR_EQUAL_TO_SEMVER:
                delegate = new LessThanOrEqualToSemverMatcher(matcher.stringMatcherData);
                break;
            case IN_LIST_SEMVER:
                delegate = new InListSemverMatcher(matcher.whitelistMatcherData.whitelist);
                break;
            case BETWEEN_SEMVER:
                delegate = new BetweenSemverMatcher(matcher.betweenStringMatcherData.start, matcher.betweenStringMatcherData.end);
                break;
            case IN_RULE_BASED_SEGMENT:
                String ruleBasedSegmentName = matcher.userDefinedSegmentMatcherData.segmentName;
                delegate = new RuleBasedSegmentMatcher(ruleBasedSegmentName);
                break;
            default:
                throw new IllegalArgumentException("Unknown matcher type: " + matcher.matcherType);
        }

        String attribute = null;
        if (matcher.keySelector != null && matcher.keySelector.attribute != null) {
            attribute = matcher.keySelector.attribute;
        }

        boolean negate = matcher.negate;


        return new AttributeMatcher(attribute, delegate, negate);
    }
}