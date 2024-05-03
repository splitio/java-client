package io.split.engine.matchers;

import io.split.engine.evaluator.EvaluationContext;

import java.util.Map;

public class EqualToSemverMatcher implements Matcher {

    private final Semver _semVer;

    public EqualToSemverMatcher(String semVer) {
        _semVer = Semver.build(semVer);
    }

    @Override
    public boolean match(Object matchValue, String bucketingKey, Map<String, Object> attributes, EvaluationContext evaluationContext) {
        if (matchValue == null || _semVer == null) {
            return false;
        }
        Semver matchSemver = Semver.build(matchValue.toString());
        if (matchSemver == null) {
            return false;
        }

        return matchSemver.Version().equals(_semVer.Version());
    }

    @Override
    public String toString() {
        StringBuilder bldr = new StringBuilder();
        bldr.append("== ");
        bldr.append(_semVer);
        return bldr.toString();
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + _semVer.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (this == obj) return true;
        if (!(obj instanceof EqualToSemverMatcher)) return false;

        EqualToSemverMatcher other = (EqualToSemverMatcher) obj;

        return _semVer == other._semVer;
    }

}
