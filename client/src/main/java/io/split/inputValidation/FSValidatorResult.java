package io.split.inputValidation;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

public class FSValidatorResult {
    private final Set<String> _flagSets;
    private final int _invalidSets;

    public FSValidatorResult(TreeSet<String> flagSets, Integer invalidSets) {
        _flagSets = flagSets;
        _invalidSets = invalidSets;
    }

    public HashSet<String> getFlagSets() {
        return new LinkedHashSet<>(_flagSets);
    }

    public int getInvalidSets() {
        return _invalidSets;
    }
}