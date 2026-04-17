package io.split.rules.matchers;

import io.split.rules.engine.EvaluationContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class CombiningMatcher {

    public enum Combiner { AND }

    private final List<AttributeMatcher> _delegates;
    private final Combiner _combiner;

    public static CombiningMatcher of(Matcher matcher) {
        return new CombiningMatcher(Combiner.AND,
                new ArrayList<>(Arrays.asList(AttributeMatcher.vanilla(matcher))));
    }

    public static CombiningMatcher of(String attribute, Matcher matcher) {
        return new CombiningMatcher(Combiner.AND,
                new ArrayList<>(Arrays.asList(new AttributeMatcher(attribute, matcher, false))));
    }

    public CombiningMatcher(Combiner combiner, List<AttributeMatcher> delegates) {
        if (delegates == null || delegates.isEmpty()) throw new IllegalArgumentException("Delegates must not be empty");
        _delegates = Collections.unmodifiableList(new ArrayList<>(delegates));
        _combiner = combiner;
    }

    public boolean match(String key, String bucketingKey, Map<String, Object> attributes, EvaluationContext context) {
        if (_delegates.isEmpty()) return false;
        switch (_combiner) {
            case AND:
                for (AttributeMatcher d : _delegates) {
                    if (!d.match(key, bucketingKey, attributes, context)) return false;
                }
                return true;
            default:
                throw new IllegalArgumentException("Unknown combiner: " + _combiner);
        }
    }

    public List<AttributeMatcher> attributeMatchers() { return _delegates; }

    @Override
    public String toString() {
        StringBuilder bldr = new StringBuilder("if");
        boolean first = true;
        for (AttributeMatcher m : _delegates) {
            if (!first) bldr.append(" ").append(_combiner);
            bldr.append(" ").append(m);
            first = false;
        }
        return bldr.toString();
    }

    @Override
    public int hashCode() { return Objects.hash(_combiner, _delegates); }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (this == obj) return true;
        if (!(obj instanceof CombiningMatcher)) return false;
        CombiningMatcher other = (CombiningMatcher) obj;
        return _combiner.equals(other._combiner) && _delegates.equals(other._delegates);
    }
}
