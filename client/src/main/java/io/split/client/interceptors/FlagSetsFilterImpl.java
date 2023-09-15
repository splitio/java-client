package io.split.client.interceptors;

import java.util.HashSet;

public class FlagSetsFilterImpl implements  FlagSetsFilter {

    private final HashSet<String> _flagSets;
    private final boolean _shouldFilter;

    public FlagSetsFilterImpl(HashSet<String> flagSets) {
        _shouldFilter = !flagSets.isEmpty();
        _flagSets = flagSets;
    }
    @Override
    public boolean Intersect(HashSet<String> sets) {
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
    public boolean Intersect(String set) {
        if (!_shouldFilter) {
            return true;
        }
        if (set.isEmpty()){
            return false;
        }
        return _flagSets.contains(set);
    }
}