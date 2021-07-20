package io.split.storages.pluggable.domain;

import io.split.client.dtos.ConditionType;
import io.split.client.dtos.Partition;

import java.util.List;

public class RawParsedCondition {
    private ConditionType _conditionType;
    private RawCombiningMatcher _matcher;
    private List<Partition> _partitions;
    private String _label;
}
