package io.split.inputValidation;

import java.util.HashSet;

public class FlagSetsValidResult {
    private final Boolean _valid;
    private final HashSet<String> _flagSets;

    public FlagSetsValidResult(Boolean valid, HashSet<String> flagSets) {
        _valid = valid;
        _flagSets = flagSets;
    }

    public Boolean getValid() {
        return _valid;
    }

    public HashSet<String> getFlagSets() {
        return _flagSets;
    }
}