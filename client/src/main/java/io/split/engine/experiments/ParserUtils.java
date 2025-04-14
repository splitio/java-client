package io.split.engine.experiments;

import com.google.common.collect.Lists;
import io.split.client.dtos.*;
import io.split.client.dtos.Matcher;
import io.split.engine.evaluator.Labels;
import io.split.engine.matchers.*;
import io.split.engine.matchers.collections.ContainsAllOfSetMatcher;
import io.split.engine.matchers.collections.ContainsAnyOfSetMatcher;
import io.split.engine.matchers.collections.EqualToSetMatcher;
import io.split.engine.matchers.collections.PartOfSetMatcher;
import io.split.engine.matchers.strings.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public final class ParserUtils {

    private static final Logger _log = LoggerFactory.getLogger(ParserUtils.class);

    public ParserUtils() {
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
        if (typeCheck != null)  return false;
        return true;
    }

    public static ParsedCondition getTemplateCondition() {
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

    public static CombiningMatcher toMatcher(MatcherGroup matcherGroup) {
        List<Matcher> matchers = matcherGroup.matchers;
        checkArgument(!matchers.isEmpty());

        List<AttributeMatcher> toCombine = Lists.newArrayList();

        for (Matcher matcher : matchers) {
            toCombine.add(toMatcher(matcher));
        }

        return new CombiningMatcher(matcherGroup.combiner, toCombine);
    }


    public static AttributeMatcher toMatcher(Matcher matcher) {
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
            case EQUAL_TO_SEMVER:
                checkNotNull(matcher.stringMatcherData, "stringMatcherData is required for EQUAL_TO_SEMVER matcher type");
                delegate = new EqualToSemverMatcher(matcher.stringMatcherData);
                break;
            case GREATER_THAN_OR_EQUAL_TO_SEMVER:
                checkNotNull(matcher.stringMatcherData, "stringMatcherData is required for GREATER_THAN_OR_EQUAL_TO_SEMVER matcher type");
                delegate = new GreaterThanOrEqualToSemverMatcher(matcher.stringMatcherData);
                break;
            case LESS_THAN_OR_EQUAL_TO_SEMVER:
                checkNotNull(matcher.stringMatcherData, "stringMatcherData is required for LESS_THAN_OR_EQUAL_SEMVER matcher type");
                delegate = new LessThanOrEqualToSemverMatcher(matcher.stringMatcherData);
                break;
            case IN_LIST_SEMVER:
                checkNotNull(matcher.whitelistMatcherData, "whitelistMatcherData is required for IN_LIST_SEMVER matcher type");
                delegate = new InListSemverMatcher(matcher.whitelistMatcherData.whitelist);
                break;
            case BETWEEN_SEMVER:
                checkNotNull(matcher.betweenStringMatcherData, "betweenStringMatcherData is required for BETWEEN_SEMVER matcher type");
                delegate = new BetweenSemverMatcher(matcher.betweenStringMatcherData.start, matcher.betweenStringMatcherData.end);
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