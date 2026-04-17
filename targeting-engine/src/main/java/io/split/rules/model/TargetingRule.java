package io.split.rules.model;

import io.split.rules.matchers.PrerequisitesMatcher;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A fully-parsed targeting rule containing only the fields needed for evaluation.
 * Metadata (name, changeNumber, trafficTypeName, configurations, flagSets, impressionsDisabled)
 * belongs in ParsedSplit (client module), not here.
 */
public final class TargetingRule {
    private final int _seed;
    private final boolean _killed;
    private final String _defaultTreatment;
    private final List<Condition> _conditions;
    private final int _trafficAllocation;
    private final int _trafficAllocationSeed;
    private final int _algo;
    private final List<Prerequisite> _prerequisites;
    private final PrerequisitesMatcher _prerequisitesMatcher;

    public TargetingRule(int seed, boolean killed, String defaultTreatment,
                         List<Condition> conditions, int trafficAllocation,
                         int trafficAllocationSeed, int algo,
                         List<Prerequisite> prerequisites) {
        _seed = seed;
        _killed = killed;
        _defaultTreatment = Objects.requireNonNull(defaultTreatment);
        _conditions = conditions != null
                ? Collections.unmodifiableList(conditions)
                : Collections.<Condition>emptyList();
        _trafficAllocation = trafficAllocation;
        _trafficAllocationSeed = trafficAllocationSeed;
        _algo = algo;
        _prerequisites = prerequisites != null
                ? Collections.unmodifiableList(prerequisites)
                : Collections.<Prerequisite>emptyList();
        _prerequisitesMatcher = new PrerequisitesMatcher(_prerequisites);
    }

    public int seed() { return _seed; }
    public boolean killed() { return _killed; }
    public String defaultTreatment() { return _defaultTreatment; }
    public List<Condition> conditions() { return _conditions; }
    public int trafficAllocation() { return _trafficAllocation; }
    public int trafficAllocationSeed() { return _trafficAllocationSeed; }
    public int algo() { return _algo; }
    public List<Prerequisite> prerequisites() { return _prerequisites; }
    public PrerequisitesMatcher prerequisitesMatcher() { return _prerequisitesMatcher; }
}
