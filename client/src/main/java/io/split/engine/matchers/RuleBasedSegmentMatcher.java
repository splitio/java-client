package io.split.engine.matchers;

import io.split.client.dtos.ExcludedSegments;
import io.split.engine.evaluator.EvaluationContext;
import io.split.engine.experiments.ParsedCondition;
import io.split.engine.experiments.ParsedRuleBasedSegment;

import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A matcher that checks if the key is part of a user defined segment. This class
 * assumes that the logic for refreshing what keys are part of a segment is delegated
 * to SegmentFetcher.
 *
 * @author adil
 */
public class RuleBasedSegmentMatcher implements Matcher {
    private final String _segmentName;

    public RuleBasedSegmentMatcher(String segmentName) {
        _segmentName = checkNotNull(segmentName);
    }

    @Override
    public boolean match(Object matchValue, String bucketingKey, Map<String, Object> attributes, EvaluationContext evaluationContext) {
        if (!(matchValue instanceof String)) {
            return false;
        }
        ParsedRuleBasedSegment parsedRuleBasedSegment = evaluationContext.getRuleBasedSegmentCache().get(_segmentName);
        if (parsedRuleBasedSegment == null) {
            return false;
        }

        if (parsedRuleBasedSegment.excludedKeys().contains(matchValue)) {
            return false;
        }

        if (matchExcludedSegments(parsedRuleBasedSegment.excludedSegments(), matchValue, bucketingKey, attributes, evaluationContext)) {
            return false;
        }

        return matchConditions(parsedRuleBasedSegment.parsedConditions(), matchValue, bucketingKey, attributes, evaluationContext);
    }

    private boolean matchExcludedSegments(List<ExcludedSegments> excludedSegments, Object matchValue, String bucketingKey, Map<String, Object> attributes, EvaluationContext evaluationContext) {
        for (ExcludedSegments excludedSegment: excludedSegments) {
            if (excludedSegment.isStandard() && evaluationContext.getSegmentCache().isInSegment(excludedSegment.name, (String) matchValue)) {
                return true;
            }

            if (excludedSegment.isRuleBased()) {
                RuleBasedSegmentMatcher excludedRbsMatcher = new RuleBasedSegmentMatcher(excludedSegment.name);
                if (excludedRbsMatcher.match(matchValue, bucketingKey, attributes, evaluationContext)) {
                    return true;
                }
            }
        }

        return  false;
    }

    private boolean matchConditions(List<ParsedCondition> conditions, Object matchValue, String bucketingKey,
                                    Map<String, Object> attributes, EvaluationContext evaluationContext) {
        for (ParsedCondition parsedCondition : conditions) {
            if (parsedCondition.matcher().match((String) matchValue, bucketingKey, attributes, evaluationContext)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + _segmentName.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (this == obj) return true;
        if (!(obj instanceof RuleBasedSegmentMatcher)) return false;

        RuleBasedSegmentMatcher other = (RuleBasedSegmentMatcher) obj;

        return _segmentName.equals(other._segmentName);
    }

    @Override
    public String toString() {
        StringBuilder bldr = new StringBuilder();
        bldr.append("in segment ");
        bldr.append(_segmentName);
        return bldr.toString();
    }

    public String getSegmentName() {
        return _segmentName;
    }
}
