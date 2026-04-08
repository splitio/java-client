package io.split.engine.experiments;

import java.util.ArrayList;
import java.util.Collections;
import io.split.client.dtos.ConditionType;
import io.split.client.dtos.Partition;
import io.split.rules.matchers.AttributeMatcher;
import io.split.rules.matchers.PrerequisitesMatcher;
import io.split.rules.model.TargetingRule;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final List<ParsedCondition> _parsedCondition;
    private final String _trafficTypeName;
    private final long _changeNumber;
    private final int _trafficAllocation;
    private final int _trafficAllocationSeed;
    private final int _algo;
    private final Map<String, String> _configurations;
    private final HashSet<String> _flagSets;
    private final boolean _impressionsDisabled;
    private PrerequisitesMatcher _prerequisitesMatcher;
    private final TargetingRule _targetingRule;

    public static ParsedSplit createParsedSplitForTests(
            String feature,
            int seed,
            boolean killed,
            String defaultTreatment,
            List<ParsedCondition> matcherAndSplits,
            String trafficTypeName,
            long changeNumber,
            int algo,
            HashSet<String> flagSets,
            boolean impressionsDisabled,
            PrerequisitesMatcher prerequisitesMatcher
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
                algo,
                null,
                flagSets,
                impressionsDisabled,
                prerequisitesMatcher,
                TargetingRuleFactory.buildTargetingRule(feature, seed, killed, defaultTreatment, matcherAndSplits, trafficTypeName,
                        changeNumber, 100, seed, algo, null, flagSets, impressionsDisabled, prerequisitesMatcher)
        );
    }

    public static ParsedSplit createParsedSplitForTests(
            String feature,
            int seed,
            boolean killed,
            String defaultTreatment,
            List<ParsedCondition> matcherAndSplits,
            String trafficTypeName,
            long changeNumber,
            int algo,
            Map<String, String> configurations,
            HashSet<String> flagSets,
            boolean impressionsDisabled,
            PrerequisitesMatcher prerequisitesMatcher
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
                algo,
                configurations,
                flagSets,
                impressionsDisabled,
                prerequisitesMatcher,
                TargetingRuleFactory.buildTargetingRule(feature, seed, killed, defaultTreatment, matcherAndSplits, trafficTypeName,
                        changeNumber, 100, seed, algo, configurations, flagSets, impressionsDisabled, prerequisitesMatcher)
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
            int algo,
            Map<String, String> configurations,
            HashSet<String> flagSets,
            boolean impressionsDisabled,
            PrerequisitesMatcher prerequisitesMatcher
    ) {
        this(feature, seed, killed, defaultTreatment, matcherAndSplits, trafficTypeName, changeNumber,
                trafficAllocation, trafficAllocationSeed, algo, configurations, flagSets,
                impressionsDisabled, prerequisitesMatcher,
                TargetingRuleFactory.buildTargetingRule(feature, seed, killed, defaultTreatment, matcherAndSplits, trafficTypeName,
                        changeNumber, trafficAllocation, trafficAllocationSeed, algo, configurations,
                        flagSets, impressionsDisabled, prerequisitesMatcher));
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
            int algo,
            Map<String, String> configurations,
            HashSet<String> flagSets,
            boolean impressionsDisabled,
            PrerequisitesMatcher prerequisitesMatcher,
            TargetingRule targetingRule
    ) {
        _split = feature;
        _seed = seed;
        _killed = killed;
        _defaultTreatment = defaultTreatment;
        _parsedCondition = Collections.unmodifiableList(new ArrayList<>(matcherAndSplits));
        _trafficTypeName = trafficTypeName;
        _changeNumber = changeNumber;
        _algo = algo;
        if (_defaultTreatment == null) {
            throw new IllegalArgumentException("DefaultTreatment is null");
        }
        _trafficAllocation = trafficAllocation;
        _trafficAllocationSeed = trafficAllocationSeed;
        _configurations = configurations;
        _flagSets = flagSets;
        _impressionsDisabled = impressionsDisabled;
        _prerequisitesMatcher = prerequisitesMatcher;
        _targetingRule = targetingRule;
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
    public HashSet<String> flagSets() {
        return _flagSets;
    }

    public Map<String, String> configurations() {
        return _configurations;
    }

    public boolean impressionsDisabled() {
        return _impressionsDisabled;
    }
    public PrerequisitesMatcher prerequisitesMatcher() { return _prerequisitesMatcher; }
    public TargetingRule targetingRule() { return _targetingRule; }

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
        result = 31 * result + (_configurations == null? 0 : _configurations.hashCode());
        result = 31 * result + (_impressionsDisabled ? 1 : 0);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (this == obj) return true;
        if (!(obj instanceof ParsedSplit)) return false;

        ParsedSplit other = (ParsedSplit) obj;
        boolean trafficTypeCond = _trafficTypeName == null ? other._trafficTypeName == null : _trafficTypeName.equals(other._trafficTypeName);
        boolean configCond = _configurations == null ? other._configurations == null : _configurations.equals(other._configurations);

        return _split.equals(other._split)
                && _seed == other._seed
                && _killed == other._killed
                && _defaultTreatment.equals(other._defaultTreatment)
                && _parsedCondition.equals(other._parsedCondition)
                && trafficTypeCond
                && _changeNumber == other._changeNumber
                && _algo == other._algo
                && configCond
                && _impressionsDisabled == other._impressionsDisabled
                && _prerequisitesMatcher == other._prerequisitesMatcher;
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
        bldr.append(", config:");
        bldr.append(_configurations);
        bldr.append(", impressionsDisabled:");
        bldr.append(_impressionsDisabled);
        bldr.append(", prerequisites:");
        bldr.append(_prerequisitesMatcher);

        return bldr.toString();

    }

    public Set<String> getSegmentsNames() {
        return parsedConditions().stream()
                .flatMap(parsedCondition -> parsedCondition.matcher().attributeMatchers().stream())
                .filter(AttributeMatcher::isUserDefinedSegmentMatcher)
                .map(am -> am.asUserDefinedSegmentMatcher().getSegmentName())
                .collect(Collectors.toSet());
    }

    public Set<String> getRuleBasedSegmentsNames() {
        return parsedConditions().stream()
                .flatMap(parsedCondition -> parsedCondition.matcher().attributeMatchers().stream())
                .filter(AttributeMatcher::isRuleBasedSegmentMatcher)
                .map(am -> am.asRuleBasedSegmentMatcher().getSegmentName())
                .collect(Collectors.toSet());
    }
}
