package io.split.client.dtos;

import java.util.Arrays;
import java.util.List;

public class RuleBasedSegment {
    public String name;
    public Status status;
    public String trafficTypeName;
    public long changeNumber;
    public List<Condition> conditions;
    public Excluded excluded;

    @Override
    public String toString() {
        return "RuleBasedSegment{" +
                "name='" + name + '\'' +
                ", status=" + status +
                ", trafficTypeName='" + trafficTypeName + '\'' +
                ", changeNumber=" + changeNumber +
                ", excluded.keys=" + Arrays.toString(excluded.keys.stream().toArray()) +
                ", excluded.segments=" + Arrays.toString(excluded.segments.stream().toArray()) +
                '}';
    }
}
