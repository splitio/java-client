package io.split.engine.matchers;

import io.split.engine.evaluator.Evaluator;

import java.util.Map;

/**
 * A matcher that matches all keys. It returns true for everything.
 *
 * @author adil
 */
public final class AllKeysMatcher implements Matcher {

    @Override
    public boolean match(Object matchValue, String bucketingKey, Map<String, Object> attributes, Evaluator evaluator) {
        if (matchValue == null) {
            return false;
        }
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (this == obj) return true;
        if (!(obj instanceof AllKeysMatcher)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        return 17;
    }

    @Override
    public String toString() {
        return "in segment all";
    }
}
