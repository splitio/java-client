package io.split.rules.matchers;

import io.split.rules.engine.EvaluationContext;

import java.util.Map;
import java.util.Objects;

/**
 * Checks if the key is a member of a rule-based segment.
 * Delegates to EvaluationContext.isInRuleBasedSegment() — the SDK provides the actual evaluation logic
 * (excluded keys, excluded segments, condition matching).
 */
public final class RuleBasedSegmentMatcher implements Matcher {
    private final String _segmentName;

    public RuleBasedSegmentMatcher(String segmentName) {
        _segmentName = Objects.requireNonNull(segmentName);
    }

    @Override
    public boolean match(Object matchValue, String bucketingKey, Map<String, Object> attributes, EvaluationContext context) {
        if (!(matchValue instanceof String)) return false;
        return context.isInRuleBasedSegment(_segmentName, (String) matchValue, bucketingKey, attributes);
    }

    public String getSegmentName() { return _segmentName; }

    @Override
    public int hashCode() { return 31 * 17 + _segmentName.hashCode(); }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (this == obj) return true;
        if (!(obj instanceof RuleBasedSegmentMatcher)) return false;
        return _segmentName.equals(((RuleBasedSegmentMatcher) obj)._segmentName);
    }

    @Override
    public String toString() { return "in rule-based segment " + _segmentName; }
}
