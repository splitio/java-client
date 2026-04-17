package io.split.client.dtos;

import java.util.ArrayList;
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
                ", changeNumber=" + changeNumber + '\'' +
                excludedToString() + '\'' +
                '}';
    }

    public String excludedToString() {
        Excluded ts = excluded != null ? excluded : new Excluded();
        if (ts.keys == null) {
            ts.keys = new ArrayList<>();
        }

        if (ts.segments == null) {
            ts.segments = new ArrayList<>();
        }

        return ", excludedKeys=" + ts.keys + '\'' + ", excludedSegments=" + ts.segments;
    }
}
