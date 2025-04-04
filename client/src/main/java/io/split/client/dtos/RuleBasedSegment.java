package io.split.client.dtos;

import java.util.List;

public class RuleBasedSegment {
    public String name;
    public Status status;
    public String trafficTypeName;
    public long changeNumber;
    public List<Condition> conditions;
    public Excluded excluded;
}
