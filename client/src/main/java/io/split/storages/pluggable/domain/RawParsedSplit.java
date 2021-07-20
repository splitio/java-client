package io.split.storages.pluggable.domain;

import io.split.engine.experiments.ParsedCondition;

import java.util.List;
import java.util.Map;

public class RawParsedSplit {
    private String _split;
    private int _seed;
    private boolean _killed;
    private String _defaultTreatment;
    private List<RawParsedCondition> _parsedCondition;
    private String _trafficTypeName;
    private long _changeNumber;
    private int _trafficAllocation;
    private int _trafficAllocationSeed;
    private int _algo;
    private Map<String, String> _configurations;

    public String get_split() {
        return _split;
    }

    public void set_split(String _split) {
        this._split = _split;
    }

    public int get_seed() {
        return _seed;
    }

    public void set_seed(int _seed) {
        this._seed = _seed;
    }

    public boolean is_killed() {
        return _killed;
    }

    public void set_killed(boolean _killed) {
        this._killed = _killed;
    }

    public String get_defaultTreatment() {
        return _defaultTreatment;
    }

    public void set_defaultTreatment(String _defaultTreatment) {
        this._defaultTreatment = _defaultTreatment;
    }

    public List<RawParsedCondition> get_parsedCondition() {
        return _parsedCondition;
    }

    public void set_parsedCondition(List<RawParsedCondition> _parsedCondition) {
        this._parsedCondition = _parsedCondition;
    }

    public String get_trafficTypeName() {
        return _trafficTypeName;
    }

    public void set_trafficTypeName(String _trafficTypeName) {
        this._trafficTypeName = _trafficTypeName;
    }

    public long get_changeNumber() {
        return _changeNumber;
    }

    public void set_changeNumber(long _changeNumber) {
        this._changeNumber = _changeNumber;
    }

    public int get_trafficAllocation() {
        return _trafficAllocation;
    }

    public void set_trafficAllocation(int _trafficAllocation) {
        this._trafficAllocation = _trafficAllocation;
    }

    public int get_trafficAllocationSeed() {
        return _trafficAllocationSeed;
    }

    public void set_trafficAllocationSeed(int _trafficAllocationSeed) {
        this._trafficAllocationSeed = _trafficAllocationSeed;
    }

    public int get_algo() {
        return _algo;
    }

    public void set_algo(int _algo) {
        this._algo = _algo;
    }

    public Map<String, String> get_configurations() {
        return _configurations;
    }

    public void set_configurations(Map<String, String> _configurations) {
        this._configurations = _configurations;
    }
}
