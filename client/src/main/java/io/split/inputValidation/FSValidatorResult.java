package io.split.inputValidation;

import java.util.HashSet;

public class FSValidatorResult {
    private final HashSet<String> _flagSets;
    private final int _invalidSets;

    public FSValidatorResult(HashSet<String> flagSets, Integer invalidSets) {
        _flagSets = flagSets;
        _invalidSets = invalidSets;
    }

    public HashSet<String> getFlagSets() {
        return _flagSets;
    }

    public int getInvalidSets() {
        return _invalidSets;
    }
}
