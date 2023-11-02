package io.split.client.interceptors;

import java.util.Set;

public class FlagSetsFilterImpl implements  FlagSetsFilter {

    private final Set<String> _flagSets;
    private final boolean _shouldFilter;

    public FlagSetsFilterImpl(Set<String> flagSets) {
        _shouldFilter = !flagSets.isEmpty();
        _flagSets = flagSets;
    }
    @Override
    public boolean intersect(Set<String> sets) {
        if (!_shouldFilter) {
            return true;
        }
        if (sets == null || sets.isEmpty()) {
            return false;
        }
        for (String set: sets) {
            if (_flagSets.contains(set)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean intersect(String set) {
        if (!_shouldFilter) {
            return true;
        }
        if (set.isEmpty()){
            return false;
        }
        return _flagSets.contains(set);
    }
}