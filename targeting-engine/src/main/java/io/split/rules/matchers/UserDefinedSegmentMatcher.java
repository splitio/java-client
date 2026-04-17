package io.split.rules.matchers;

import io.split.rules.engine.EvaluationContext;

import java.util.Map;
import java.util.Objects;

/**
 * Checks if the key is a member of a standard (user-defined) segment.
 * Delegates to EvaluationContext.isInSegment() — the SDK provides the actual storage lookup.
 */
public final class UserDefinedSegmentMatcher implements Matcher {
    private final String _segmentName;

    public UserDefinedSegmentMatcher(String segmentName) {
        _segmentName = Objects.requireNonNull(segmentName);
    }

    @Override
    public boolean match(Object matchValue, String bucketingKey, Map<String, Object> attributes, EvaluationContext context) {
        if (!(matchValue instanceof String)) return false;
        return context.isInSegment(_segmentName, (String) matchValue);
    }

    public String getSegmentName() { return _segmentName; }

    @Override
    public int hashCode() { return 31 * 17 + _segmentName.hashCode(); }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (this == obj) return true;
        if (!(obj instanceof UserDefinedSegmentMatcher)) return false;
        return _segmentName.equals(((UserDefinedSegmentMatcher) obj)._segmentName);
    }

    @Override
    public String toString() { return "in segment " + _segmentName; }
}
