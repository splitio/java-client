package io.split.rules.model;

import io.split.rules.matchers.CombiningMatcher;

import java.util.Collections;
import java.util.List;

public final class Condition {
    private final ConditionType _conditionType;
    private final CombiningMatcher _matcher;
    private final List<Partition> _partitions;
    private final String _label;

    public Condition(ConditionType conditionType, CombiningMatcher matcher, List<Partition> partitions, String label) {
        _conditionType = conditionType;
        _matcher = matcher;
        _partitions = partitions != null ? Collections.unmodifiableList(partitions) : Collections.<Partition>emptyList();
        _label = label;
    }

    public ConditionType conditionType() {
        return _conditionType;
    }

    public CombiningMatcher matcher() {
        return _matcher;
    }

    public List<Partition> partitions() {
        return _partitions;
    }

    public String label() {
        return _label;
    }
}
