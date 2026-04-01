package io.split.rules.matchers.collections;

import io.split.rules.engine.EvaluationContext;
import io.split.rules.matchers.Matcher;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static io.split.rules.matchers.Transformers.toSetOfStrings;

/**
 * Created by adilaijaz on 3/7/16.
 */
public class PartOfSetMatcher implements Matcher {

    private final Set<String> _compareTo = new HashSet<>();

    public PartOfSetMatcher(Collection<String> compareTo) {
        if (compareTo == null) {
            throw new IllegalArgumentException("Null whitelist");
        }
        _compareTo.addAll(compareTo);
    }

    @Override
    public boolean match(Object matchValue, String bucketingKey, Map<String, Object> attributes, EvaluationContext evaluationContext) {
        if (matchValue == null) {
            return false;
        }

        if (!(matchValue instanceof Collection)) {
            return false;
        }

        Set<String> keyAsSet = toSetOfStrings((Collection) matchValue);

        if (keyAsSet.isEmpty()) {
            return false;
        }

        return _compareTo.containsAll(keyAsSet);
    }

    @Override
    public String toString() {
        StringBuilder bldr = new StringBuilder();
        bldr.append("is part of ");
        bldr.append(_compareTo);
        return bldr.toString();
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + _compareTo.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (this == obj) return true;
        if (!(obj instanceof PartOfSetMatcher)) return false;

        PartOfSetMatcher other = (PartOfSetMatcher) obj;

        return _compareTo.equals(other._compareTo);
    }

}
