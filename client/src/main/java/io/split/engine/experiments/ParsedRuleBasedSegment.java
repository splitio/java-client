package io.split.engine.experiments;

import com.google.common.collect.ImmutableList;
import io.split.client.dtos.ExcludedSegments;
import io.split.engine.matchers.AttributeMatcher;
import io.split.engine.matchers.UserDefinedSegmentMatcher;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ParsedRuleBasedSegment {

    private final String _ruleBasedSegment;
    private final ImmutableList<ParsedCondition> _parsedCondition;
    private final String _trafficTypeName;
    private final long _changeNumber;
    private final List<String> _excludedKeys;
    private final List<ExcludedSegments> _excludedSegments;

    public static ParsedRuleBasedSegment createParsedRuleBasedSegmentForTests(
            String ruleBasedSegment,
            List<ParsedCondition> matcherAndSplits,
            String trafficTypeName,
            long changeNumber,
            List<String> excludedKeys,
            List<ExcludedSegments> excludedSegments
    ) {
        return new ParsedRuleBasedSegment(
                ruleBasedSegment,
                matcherAndSplits,
                trafficTypeName,
                changeNumber,
                excludedKeys,
                excludedSegments
        );
    }

    public ParsedRuleBasedSegment(
            String ruleBasedSegment,
            List<ParsedCondition> matcherAndSplits,
            String trafficTypeName,
            long changeNumber,
            List<String> excludedKeys,
            List<ExcludedSegments> excludedSegments
    ) {
        _ruleBasedSegment = ruleBasedSegment;
        _parsedCondition = ImmutableList.copyOf(matcherAndSplits);
        _trafficTypeName = trafficTypeName;
        _changeNumber = changeNumber;
        _excludedKeys = excludedKeys;
        _excludedSegments = excludedSegments;
    }

    public String ruleBasedSegment() {
        return _ruleBasedSegment;
    }

    public List<ParsedCondition> parsedConditions() {
        return _parsedCondition;
    }

    public String trafficTypeName() {return _trafficTypeName;}

    public long changeNumber() {return _changeNumber;}

    public List<String> excludedKeys() {return _excludedKeys;}
    public List<ExcludedSegments> excludedSegments() {return _excludedSegments;}

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + _ruleBasedSegment.hashCode();
        result = 31 * result + _parsedCondition.hashCode();
        result = 31 * result + (_trafficTypeName == null ? 0 : _trafficTypeName.hashCode());
        result = 31 * result + (int)(_changeNumber ^ (_changeNumber >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (this == obj) return true;
        if (!(obj instanceof ParsedRuleBasedSegment)) return false;

        ParsedRuleBasedSegment other = (ParsedRuleBasedSegment) obj;

        return _ruleBasedSegment.equals(other._ruleBasedSegment)
                && _parsedCondition.equals(other._parsedCondition)
                && _trafficTypeName == null ? other._trafficTypeName == null : _trafficTypeName.equals(other._trafficTypeName)
                && _changeNumber == other._changeNumber;
    }

    @Override
    public String toString() {
        StringBuilder bldr = new StringBuilder();
        bldr.append("name:");
        bldr.append(_ruleBasedSegment);
        bldr.append(", parsedConditions:");
        bldr.append(_parsedCondition);
        bldr.append(", trafficTypeName:");
        bldr.append(_trafficTypeName);
        bldr.append(", changeNumber:");
        bldr.append(_changeNumber);
        return bldr.toString();

    }

    public Set<String> getSegmentsNames() {
        Set<String> segmentNames = excludedSegments()
                .stream()
                .filter(ExcludedSegments::isStandard)
                .map(ExcludedSegments::getSegmentName)
                .collect(Collectors.toSet());

        segmentNames.addAll(parsedConditions().stream()
                .flatMap(parsedCondition -> parsedCondition.matcher().attributeMatchers().stream())
                .filter(ParsedRuleBasedSegment::isSegmentMatcher)
                .map(ParsedRuleBasedSegment::asSegmentMatcherForEach)
                .map(UserDefinedSegmentMatcher::getSegmentName)
                .collect(Collectors.toSet()));

        return  segmentNames;
    }

    private static boolean isSegmentMatcher(AttributeMatcher attributeMatcher) {
        return ((AttributeMatcher.NegatableMatcher) attributeMatcher.matcher()).delegate() instanceof UserDefinedSegmentMatcher;
    }

    private static UserDefinedSegmentMatcher asSegmentMatcherForEach(AttributeMatcher attributeMatcher) {
        return (UserDefinedSegmentMatcher) ((AttributeMatcher.NegatableMatcher) attributeMatcher.matcher()).delegate();
    }

}
