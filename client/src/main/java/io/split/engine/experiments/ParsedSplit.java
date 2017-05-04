package io.split.engine.experiments;

import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * a value class representing an io.codigo.dtos.Experiment. Why are we not using
 * that class? Because it does not have the logic of matching. ParsedExperiment
 * has the matchers that also encapsulate the logic of matching. We
 * can easily cache this object.
 *
 * @author adil
 */
public class ParsedSplit {

    private final String _split;
    private final int _seed;
    private final boolean _killed;
    private final String _defaultTreatment;
    private final ImmutableList<ParsedCondition> _parsedCondition;
    private final String _trafficTypeName;
    private final long _changeNumber;
    private final int _trafficAllocation;
    private final int _trafficAllocationSeed;
    private final int _algo;

    public static ParsedSplit createParsedSplitForTests(
            String feature,
            int seed,
            boolean killed,
            String defaultTreatment,
            List<ParsedCondition> matcherAndSplits,
            String trafficTypeName,
            long changeNumber,
            int algo
    ) {
        return new ParsedSplit(
                feature,
                seed,
                killed,
                defaultTreatment,
                matcherAndSplits,
                trafficTypeName,
                changeNumber,
                100,
                seed,
                algo
        );
    }

    public ParsedSplit(
            String feature,
            int seed,
            boolean killed,
            String defaultTreatment,
            List<ParsedCondition> matcherAndSplits,
            String trafficTypeName,
            long changeNumber,
            int trafficAllocation,
            int trafficAllocationSeed,
            int algo
    ) {
        _split = feature;
        _seed = seed;
        _killed = killed;
        _defaultTreatment = defaultTreatment;
        _parsedCondition = ImmutableList.copyOf(matcherAndSplits);
        _trafficTypeName = trafficTypeName;
        _changeNumber = changeNumber;
        _algo = algo;
        if (_defaultTreatment == null) {
            throw new IllegalArgumentException("DefaultTreatment is null");
        }
        this._trafficAllocation = trafficAllocation;
        this._trafficAllocationSeed = trafficAllocationSeed;
    }



    public String feature() {
        return _split;
    }

    public int trafficAllocation() {
        return _trafficAllocation;
    }

    public int trafficAllocationSeed() {
        return _trafficAllocationSeed;
    }

    public int seed() {
        return _seed;
    }

    public boolean killed() {
        return _killed;
    }

    public String defaultTreatment() {
        return _defaultTreatment;
    }

    public List<ParsedCondition> parsedConditions() {
        return _parsedCondition;
    }

    public String trafficTypeName() {return _trafficTypeName;}

    public long changeNumber() {return _changeNumber;}

    public int algo() {return _algo;}

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + _split.hashCode();
        result = 31 * result + (int)(_seed ^ (_seed >>> 32));
        result = 31 * result + (_killed ? 1 : 0);
        result = 31 * result + _defaultTreatment.hashCode();
        result = 31 * result + _parsedCondition.hashCode();
        result = 31 * result + (_trafficTypeName == null ? 0 : _trafficTypeName.hashCode());
        result = 31 * result + (int)(_changeNumber ^ (_changeNumber >>> 32));
        result = 31 * result + (_algo ^ (_algo >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (this == obj) return true;
        if (!(obj instanceof ParsedSplit)) return false;

        ParsedSplit other = (ParsedSplit) obj;

        return _split.equals(other._split)
                && _seed == other._seed
                && _killed == other._killed
                && _defaultTreatment.equals(other._defaultTreatment)
                && _parsedCondition.equals(other._parsedCondition)
                && _trafficTypeName == null ? other._trafficTypeName == null : _trafficTypeName.equals(other._trafficTypeName)
                && _changeNumber == other._changeNumber
                && _algo == other._algo;
    }

    @Override
    public String toString() {
        StringBuilder bldr = new StringBuilder();
        bldr.append("name:");
        bldr.append(_split);
        bldr.append(", seed:");
        bldr.append(_seed);
        bldr.append(", killed:");
        bldr.append(_killed);
        bldr.append(", default treatment:");
        bldr.append(_defaultTreatment);
        bldr.append(", parsedConditions:");
        bldr.append(_parsedCondition);
        bldr.append(", trafficTypeName:");
        bldr.append(_trafficTypeName);
        bldr.append(", changeNumber:");
        bldr.append(_changeNumber);
        bldr.append(", algo:");
        bldr.append(_algo);
        return bldr.toString();

    }
}
