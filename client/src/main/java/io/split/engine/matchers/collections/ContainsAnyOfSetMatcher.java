package io.split.engine.matchers.collections;

import io.split.engine.matchers.Matcher;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by adilaijaz on 3/7/16.
 */
public class ContainsAnyOfSetMatcher implements Matcher {

    private final Set<String> _compareTo = new HashSet<>();

    public ContainsAnyOfSetMatcher(Collection<String> compareTo) {
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

        if (!(key instanceof Collection)) {
            return false;
        }

        for (String s : _compareTo) {
            if ((((Collection) key).contains(s))) {
                return true;
            }
        }

        return false;
    }



    @Override
    public String toString() {
        StringBuilder bldr = new StringBuilder();
        bldr.append("contains any of ");
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
        if (!(obj instanceof ContainsAnyOfSetMatcher)) return false;

        ContainsAnyOfSetMatcher other = (ContainsAnyOfSetMatcher) obj;

        return _compareTo.equals(other._compareTo);
    }

}
