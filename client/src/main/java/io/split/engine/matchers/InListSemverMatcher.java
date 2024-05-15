package io.split.engine.matchers;

import io.split.engine.evaluator.EvaluationContext;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class InListSemverMatcher implements Matcher {

    private final Set<Semver> _semverlist = new HashSet<>();

    public InListSemverMatcher(Collection<String> whitelist) {
        for (String item : whitelist) {
            Semver semver = Semver.build(item);
            if (semver == null) continue;

            _semverlist.add(semver);
        }
    }

    @Override
    public boolean match(Object matchValue, String bucketingKey, Map<String, Object> attributes, EvaluationContext evaluationContext) {
        if (!(matchValue instanceof String) || _semverlist.isEmpty()) {
            return false;
        }
        Semver matchSemver = Semver.build(matchValue.toString());
        if (matchSemver == null) {
            return false;
        }

        for (Semver semverItem : _semverlist) {
            if (semverItem.version().equals(matchSemver.version())) return true;
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder bldr = new StringBuilder();
        bldr.append("in semver list [");
        boolean first = true;

        for (Semver item : _semverlist) {
            if (!first) {
                bldr.append(',');
            }
            bldr.append('"');
            bldr.append(item.version());
            bldr.append('"');
            first = false;
        }

        bldr.append("]");
        return bldr.toString();
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + _semverlist.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (this == obj) return true;
        if (!(obj instanceof InListSemverMatcher)) return false;

        InListSemverMatcher other = (InListSemverMatcher) obj;

        return _semverlist == other._semverlist;
    }

}
