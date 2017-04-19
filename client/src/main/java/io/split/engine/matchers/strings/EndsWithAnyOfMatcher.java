package io.split.engine.matchers.strings;

import io.split.engine.matchers.Matcher;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by adilaijaz on 3/7/16.
 */
public class EndsWithAnyOfMatcher implements Matcher {

    private final Set<String> _compareTo = new HashSet<>();

    public EndsWithAnyOfMatcher(Collection<String> compareTo) {
        if (compareTo == null) {
            throw new IllegalArgumentException("Null whitelist");
        }
        _compareTo.addAll(compareTo);
    }

    @Override
    public boolean match(Object key) {

        if (key == null) {
            return false;
        }

        if (!(key instanceof String) ) {
            return false;
        }

        if (_compareTo.isEmpty()) {
            return false;
        }

        String keyAsString = (String) key;

        for (String s : _compareTo) {
            if (s.isEmpty()) {
                // ignore empty strings.
                continue;
            }
            if (keyAsString.endsWith(s)) {
                return true;
            }
        }

        return false;
    }



    @Override
    public String toString() {
        StringBuilder bldr = new StringBuilder();
        bldr.append("ends with ");
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
        if (!(obj instanceof EndsWithAnyOfMatcher)) return false;

        EndsWithAnyOfMatcher other = (EndsWithAnyOfMatcher) obj;

        return _compareTo.equals(other._compareTo);
    }

}
