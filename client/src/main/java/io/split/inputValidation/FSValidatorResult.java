package io.split.inputValidation;

import java.util.List;

public class FSValidatorResult {
    private final List<String> _flagSets;
    private final int _invalidSets;

    public FSValidatorResult(List<String> flagSets, Integer invalidSets) {
        _flagSets = flagSets;
        _invalidSets = invalidSets;
    }

    public List<String> getFlagSets() {
        return _flagSets;
    }

    public int getInvalidSets() {
        return _invalidSets;
    }
}
