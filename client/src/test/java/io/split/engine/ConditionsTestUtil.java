package io.split.engine;

import com.google.common.collect.Lists;
import io.split.client.dtos.*;

import java.util.List;

/**
 * Utility methods for creating conditions for testing purposes.
 *
 * @author adil
 */
public class ConditionsTestUtil {

    public static Condition and(Matcher matcher1, List<Partition> partitions) {
        MatcherGroup matcherGroup = new MatcherGroup();
        matcherGroup.combiner = MatcherCombiner.AND;
        matcherGroup.matchers = Lists.newArrayList(matcher1);

        Condition c = new Condition();
        c.matcherGroup = matcherGroup;
        c.partitions = partitions;

        return c;
    }

    public static Condition and(Matcher matcher1, Matcher matcher2, List<Partition> partitions) {
        MatcherGroup matcherGroup = new MatcherGroup();
        matcherGroup.combiner = MatcherCombiner.AND;
        matcherGroup.matchers = Lists.newArrayList(matcher1, matcher2);

        Condition c = new Condition();
        c.matcherGroup = matcherGroup;
        c.partitions = partitions;

        return c;
    }

    public static Condition makeWhitelistCondition(ConditionType conditionType, List<String> whitelist, List<Partition> partitions) {
        return makeWhitelistCondition(conditionType, whitelist, partitions, false);
    }

    public static Condition makeWhitelistCondition(ConditionType conditionType, List<String> whitelist, List<Partition> partitions, boolean negate) {
        Matcher matcher = whitelistMatcher(whitelist, negate);

        MatcherGroup matcherGroup = new MatcherGroup();
        matcherGroup.combiner = MatcherCombiner.AND;
        matcherGroup.matchers = Lists.newArrayList(matcher);

        Condition c = new Condition();
        c.conditionType = conditionType;
        c.matcherGroup = matcherGroup;
        c.partitions = partitions;

        return c;
    }

    public static Matcher whitelistMatcher(List<String> whitelist, boolean negate) {
        return whitelistMatcher(null, null, whitelist, negate);
    }

    public static Matcher whitelistMatcher(String trafficType, String attribute, List<String> whitelist, boolean negate) {
        WhitelistMatcherData whitelistMatcherData = new WhitelistMatcherData();
        whitelistMatcherData.whitelist = whitelist;

        KeySelector keySelector = null;
        if (trafficType != null && attribute != null) {
            keySelector = new KeySelector();
            keySelector.trafficType = trafficType;
            keySelector.attribute = attribute;
        }
        Matcher matcher = new Matcher();
        matcher.keySelector = keySelector;
        matcher.matcherType = MatcherType.WHITELIST;
        matcher.negate = negate;
        matcher.whitelistMatcherData = whitelistMatcherData;
        matcher.negate = negate;
        return matcher;
    }

    public static Condition makeUserDefinedSegmentCondition(ConditionType conditionType, String segment, List<Partition> partitions) {
        return makeUserDefinedSegmentCondition(conditionType, segment, partitions, false);
    }

    public static Condition makeUserDefinedSegmentCondition(ConditionType conditionType, String segment, List<Partition> partitions, boolean negate) {
        Matcher matcher = userDefinedSegmentMatcher(segment, negate);

        MatcherGroup matcherGroup = new MatcherGroup();
        matcherGroup.combiner = MatcherCombiner.AND;
        matcherGroup.matchers = Lists.newArrayList(matcher);

        Condition c = new Condition();
        c.conditionType = conditionType;
        c.matcherGroup = matcherGroup;
        c.partitions = partitions;

        return c;
    }

    public static Matcher userDefinedSegmentMatcher(String segment, boolean negate) {
        return userDefinedSegmentMatcher(null, null, segment, negate);
    }

    public static Matcher userDefinedSegmentMatcher(String trafficType, String attribute, String segment, boolean negate) {
        UserDefinedSegmentMatcherData userDefinedSegment = new UserDefinedSegmentMatcherData();
        userDefinedSegment.segmentName = segment;

        KeySelector keySelector = null;
        if (trafficType != null && attribute != null) {
            keySelector = new KeySelector();
            keySelector.trafficType = trafficType;
            keySelector.attribute = attribute;
        }

        Matcher matcher = new Matcher();
        matcher.keySelector = keySelector;
        matcher.matcherType = MatcherType.IN_SEGMENT;
        matcher.negate = negate;
        matcher.userDefinedSegmentMatcherData = userDefinedSegment;
        return matcher;
    }

    public static Condition makeAllKeysCondition(List<Partition> partitions) {
        return makeAllKeysCondition(partitions, false);
    }

    public static Condition makeAllKeysCondition(List<Partition> partitions, boolean negate) {
        Matcher matcher = allKeysMatcher(negate);

        MatcherGroup matcherGroup = new MatcherGroup();
        matcherGroup.combiner = MatcherCombiner.AND;
        matcherGroup.matchers = Lists.newArrayList(matcher);

        Condition c = new Condition();
        c.conditionType = ConditionType.ROLLOUT;
        c.matcherGroup = matcherGroup;
        c.partitions = partitions;

        return c;
    }

    public static Matcher allKeysMatcher(boolean negate) {
        Matcher matcher = new Matcher();
        matcher.matcherType = MatcherType.ALL_KEYS;
        matcher.negate = negate;
        return matcher;
    }

    public static Matcher numericMatcher(String trafficType, String attribute,
                                         MatcherType matcherType, DataType dataType,
                                         long number, boolean negate) {

        KeySelector keySelector = new KeySelector();
        keySelector.trafficType = trafficType;
        keySelector.attribute = attribute;

        UnaryNumericMatcherData numericMatcherData = new UnaryNumericMatcherData();
        numericMatcherData.dataType = dataType;
        numericMatcherData.value = number;

        Matcher matcher = new Matcher();
        matcher.keySelector = keySelector;
        matcher.matcherType = matcherType;
        matcher.negate = negate;
        matcher.unaryNumericMatcherData = numericMatcherData;

        return matcher;
    }

    public static Matcher betweenMatcher(String trafficType,
                                         String attribute,
                                         DataType dataType,
                                         long start,
                                         long end,
                                         boolean negate) {

        KeySelector keySelector = new KeySelector();
        keySelector.trafficType = trafficType;
        keySelector.attribute = attribute;

        BetweenMatcherData betweenMatcherData = new BetweenMatcherData();
        betweenMatcherData.dataType = dataType;
        betweenMatcherData.start = start;
        betweenMatcherData.end = end;

        Matcher matcher = new Matcher();
        matcher.keySelector = keySelector;
        matcher.matcherType = MatcherType.BETWEEN;
        matcher.negate = negate;
        matcher.betweenMatcherData = betweenMatcherData;

        return matcher;
    }

    public static Partition partition(String treatment, int size) {
        Partition p = new Partition();
        p.treatment = treatment;
        p.size = size;
        return p;
    }
}
