package io.split.rules.matchers;

import io.split.rules.engine.EvaluationContext;

import java.util.Map;
import java.util.Objects;

public final class AttributeMatcher {
    private final String _attribute;
    private final Matcher _matcher;

    public static AttributeMatcher vanilla(Matcher matcher) {
        return new AttributeMatcher(null, matcher, false);
    }

    public AttributeMatcher(String attribute, Matcher matcher, boolean negate) {
        _attribute = attribute;
        if (matcher == null) throw new IllegalArgumentException("Null matcher");
        _matcher = new NegatableMatcher(matcher, negate);
    }

    public boolean match(String key, String bucketingKey, Map<String, Object> attributes, EvaluationContext context) {
        if (_attribute == null) {
            return _matcher.match(key, bucketingKey, attributes, context);
        }
        if (attributes == null) return false;
        Object value = attributes.get(_attribute);
        if (value == null) return false;
        return _matcher.match(value, bucketingKey, null, null);
    }

    public String attribute() { return _attribute; }
    public Matcher matcher() { return _matcher; }

    public boolean isUserDefinedSegmentMatcher() {
        return ((NegatableMatcher) _matcher).delegate() instanceof UserDefinedSegmentMatcher;
    }

    public UserDefinedSegmentMatcher asUserDefinedSegmentMatcher() {
        return (UserDefinedSegmentMatcher) ((NegatableMatcher) _matcher).delegate();
    }

    public boolean isRuleBasedSegmentMatcher() {
        return ((NegatableMatcher) _matcher).delegate() instanceof RuleBasedSegmentMatcher;
    }

    public RuleBasedSegmentMatcher asRuleBasedSegmentMatcher() {
        return (RuleBasedSegmentMatcher) ((NegatableMatcher) _matcher).delegate();
    }

    @Override
    public int hashCode() { return Objects.hash(_attribute, _matcher); }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (this == obj) return true;
        if (!(obj instanceof AttributeMatcher)) return false;
        AttributeMatcher other = (AttributeMatcher) obj;
        return Objects.equals(_attribute, other._attribute) && _matcher.equals(other._matcher);
    }

    @Override
    public String toString() {
        StringBuilder bldr = new StringBuilder("key");
        if (_attribute != null) bldr.append(".").append(_attribute);
        bldr.append(" is").append(_matcher);
        return bldr.toString();
    }

    public static final class NegatableMatcher implements Matcher {
        private final boolean _negate;
        private final Matcher _delegate;

        public NegatableMatcher(Matcher matcher, boolean negate) {
            _negate = negate;
            _delegate = matcher;
        }

        @Override
        public boolean match(Object matchValue, String bucketingKey, Map<String, Object> attributes, EvaluationContext context) {
            boolean result = _delegate.match(matchValue, bucketingKey, attributes, context);
            return _negate ? !result : result;
        }

        public Matcher delegate() { return _delegate; }

        @Override
        public int hashCode() { return Objects.hash(_negate, _delegate); }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) return false;
            if (this == obj) return true;
            if (!(obj instanceof NegatableMatcher)) return false;
            NegatableMatcher other = (NegatableMatcher) obj;
            return _negate == other._negate && _delegate.equals(other._delegate);
        }

        @Override
        public String toString() {
            return (_negate ? " not " : " ") + _delegate;
        }
    }
}
