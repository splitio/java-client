package io.split.engine.matchers.strings;

import io.split.engine.evaluator.EvaluationContext;
import io.split.engine.matchers.Matcher;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by adilaijaz on 5/4/15.
 */
public class WhitelistMatcher implements Matcher {
    private final Set<String> _whitelist = new HashSet<>();

    public WhitelistMatcher(Collection<String> whitelist) {
        if (whitelist == null) {
            throw new IllegalArgumentException("Null whitelist parameter");
        }
        _whitelist.addAll(whitelist);
    }

    @Override
    public boolean match(Object matchValue, String bucketingKey, Map<String, Object> attributes, EvaluationContext evaluationContext) {
        return _whitelist.contains(matchValue);
    }

    @Override
    public String toString() {
        StringBuilder bldr = new StringBuilder();
        bldr.append("in segment [");
        boolean first = true;

        for (String item : _whitelist) {
            if (!first) {
                bldr.append(',');
            }
            bldr.append('"');
            bldr.append(item);
            bldr.append('"');
            first = false;
        }

        bldr.append("]");
        return bldr.toString();
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + _whitelist.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (this == obj) return true;
        if (!(obj instanceof WhitelistMatcher)) return false;

        WhitelistMatcher other = (WhitelistMatcher) obj;

        return _whitelist.equals(other._whitelist);
    }

}
