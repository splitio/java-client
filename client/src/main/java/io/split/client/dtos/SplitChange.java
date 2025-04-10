package io.split.client.dtos;

import java.util.List;

public class SplitChange {
    public List<Split> splits;
    public long since;
    public long till;
    public List<RuleBasedSegment> ruleBasedSegments;
    public long sinceRBS;
    public long tillRBS;
}
