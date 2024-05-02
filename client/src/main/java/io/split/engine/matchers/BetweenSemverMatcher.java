package io.split.engine.matchers;

import io.split.engine.evaluator.EvaluationContext;

import java.util.Map;

public class BetweenSemverMatcher implements Matcher {

    private final Semver _semverStart;
    private final Semver _semverEnd;

    public BetweenSemverMatcher(String semverStart, String semverEnd) {
        _semverStart = Semver.build(semverStart);
        _semverEnd = Semver.build(semverEnd);
    }

    @Override
    public boolean match(Object matchValue, String bucketingKey, Map<String, Object> attributes, EvaluationContext evaluationContext) {
        if (matchValue == null || _semverStart == null || _semverEnd == null) {
            return false;
        }
        Semver matchSemver = Semver.build(matchValue.toString());
        if (matchSemver == null) {
            return false;
        }

        return matchSemver.Compare(_semverStart) >= 0 && matchSemver.Compare(_semverEnd) <= 0;
    }

    @Override
    public String toString() {
        StringBuilder bldr = new StringBuilder();
        bldr.append("between semver ");
        bldr.append(_semverStart.Version());
        bldr.append(" and ");
        bldr.append(_semverEnd.Version());
        return bldr.toString();
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + _semverStart.hashCode() + _semverEnd.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (this == obj) return true;
        if (!(obj instanceof BetweenSemverMatcher)) return false;

        BetweenSemverMatcher other = (BetweenSemverMatcher) obj;

        return _semverStart == other._semverStart && _semverEnd == other._semverEnd;
    }

}
