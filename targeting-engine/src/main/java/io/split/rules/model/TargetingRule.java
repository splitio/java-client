package io.split.rules.model;

import io.split.rules.matchers.PrerequisitesMatcher;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A fully-parsed targeting rule (analogous to ParsedSplit).
 * Contains all the information needed to evaluate feature flag targeting.
 */
public final class TargetingRule {
    private final String _name;
    private final int _seed;
    private final boolean _killed;
    private final String _defaultTreatment;
    private final List<Condition> _conditions;
    private final String _trafficTypeName;
    private final long _changeNumber;
    private final int _trafficAllocation;
    private final int _trafficAllocationSeed;
    private final int _algo;
    private final Map<String, String> _configurations;
    private final Set<String> _flagSets;
    private final boolean _impressionsDisabled;
    private final List<Prerequisite> _prerequisites;
    private final PrerequisitesMatcher _prerequisitesMatcher;

    public TargetingRule(String name, int seed, boolean killed, String defaultTreatment,
                         List<Condition> conditions, String trafficTypeName, long changeNumber,
                         int trafficAllocation, int trafficAllocationSeed, int algo,
                         Map<String, String> configurations, Set<String> flagSets,
                         boolean impressionsDisabled, List<Prerequisite> prerequisites) {
        _name = Objects.requireNonNull(name);
        _seed = seed;
        _killed = killed;
        _defaultTreatment = Objects.requireNonNull(defaultTreatment);
        _conditions = conditions != null
                ? Collections.unmodifiableList(conditions)
                : Collections.<Condition>emptyList();
        _trafficTypeName = trafficTypeName;
        _changeNumber = changeNumber;
        _trafficAllocation = trafficAllocation;
        _trafficAllocationSeed = trafficAllocationSeed;
        _algo = algo;
        _configurations = configurations;
        _flagSets = flagSets;
        _impressionsDisabled = impressionsDisabled;
        _prerequisites = prerequisites != null
                ? Collections.unmodifiableList(prerequisites)
                : Collections.<Prerequisite>emptyList();
        _prerequisitesMatcher = new PrerequisitesMatcher(_prerequisites);
    }

    public String name() { return _name; }
    public int seed() { return _seed; }
    public boolean killed() { return _killed; }
    public String defaultTreatment() { return _defaultTreatment; }
    public List<Condition> conditions() { return _conditions; }
    public String trafficTypeName() { return _trafficTypeName; }
    public long changeNumber() { return _changeNumber; }
    public int trafficAllocation() { return _trafficAllocation; }
    public int trafficAllocationSeed() { return _trafficAllocationSeed; }
    public int algo() { return _algo; }
    public Map<String, String> configurations() { return _configurations; }
    public Set<String> flagSets() { return _flagSets; }
    public boolean impressionsDisabled() { return _impressionsDisabled; }
    public List<Prerequisite> prerequisites() { return _prerequisites; }
    public PrerequisitesMatcher prerequisitesMatcher() { return _prerequisitesMatcher; }
}
