package io.split.engine.matchers;

import com.google.common.collect.Sets;

import java.util.Collection;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by adilaijaz on 5/4/15.
 */
public class WhitelistMatcher implements Matcher {

    private final Set<String> _whitelist = Sets.newHashSet();

    public WhitelistMatcher(Collection<String> whitelist) {
        checkNotNull(whitelist);
        _whitelist.addAll(whitelist);
    }


    @Override
    public boolean match(Object key) {
        return _whitelist.contains(key);
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
