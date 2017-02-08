package io.split.engine.matchers;

import java.util.Map;

/**
 * A matcher that matches all keys. It returns true for everything.
 *
 * @author adil
 */
public final class AllKeysMatcher implements Matcher {

    @Override
    public boolean match(Object key) {
        if (key == null) {
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
