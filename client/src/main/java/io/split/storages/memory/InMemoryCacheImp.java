package io.split.storages.memory;

import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import io.split.client.interceptors.FlagSetsFilter;
import io.split.engine.experiments.ParsedSplit;
import io.split.storages.SplitCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class InMemoryCacheImp implements SplitCache {

    private static final Logger _log = LoggerFactory.getLogger(InMemoryCacheImp.class);

    private final ConcurrentMap<String, ParsedSplit> _concurrentMap;
    private final ConcurrentMap<String, HashSet<String>> _flagSets;
    private final Multiset<String> _concurrentTrafficTypeNameSet;
    private final FlagSetsFilter _flagSetsFilter;

    private AtomicLong _changeNumber;

    public InMemoryCacheImp(FlagSetsFilter flagSets) {
        this(-1, flagSets);
    }

    public InMemoryCacheImp(long startingChangeNumber, FlagSetsFilter flagSets) {
        _concurrentMap = Maps.newConcurrentMap();
        _changeNumber = new AtomicLong(startingChangeNumber);
        _concurrentTrafficTypeNameSet = ConcurrentHashMultiset.create();
        _flagSets = Maps.newConcurrentMap();
        _flagSetsFilter = flagSets;
    }

    @Override
    public boolean remove(String name) {
        ParsedSplit removed = _concurrentMap.remove(name);
        if (removed != null) {
            removeFromFlagSets(removed.feature());
            if (removed.trafficTypeName() != null) {
                this.decreaseTrafficType(removed.trafficTypeName());
            }
        }
        return removed != null;
    }

    @Override
    public ParsedSplit get(String name) {
        return _concurrentMap.get(name);
    }

    @Override
    public Collection<ParsedSplit> getAll() {
        return _concurrentMap.values();
    }

    @Override
    public Map<String,ParsedSplit> fetchMany(List<String> names) {
        Map<String, ParsedSplit> splits = new HashMap<>();

        names.forEach(s -> splits.put(s, _concurrentMap.get(s)));

        return splits;
    }

    @Override
    public long getChangeNumber() {
        return _changeNumber.get();
    }

    @Override
    public void setChangeNumber(long changeNumber) {
        if (changeNumber < _changeNumber.get()) {
            _log.error("ChangeNumber for feature flags cache is less than previous");
        }

        _changeNumber.set(changeNumber);
    }

    @Override
    public boolean trafficTypeExists(String trafficTypeName) {
        // If the multiset has [{"user",2}.{"account",0}], elementSet only returns
        // ["user"] (it ignores "account")
        return Sets.newHashSet(_concurrentTrafficTypeNameSet.elementSet()).contains(trafficTypeName);
    }

    @Override
    public List<String> splitNames() {
        List<String> splitNamesList = new ArrayList<>();
        for (String key: _concurrentMap.keySet()) {
            splitNamesList.add(_concurrentMap.get(key).feature());
        }
        return splitNamesList;
    }

    @Override
    public Map<String, HashSet<String>> getNamesByFlagSets(List<String> flagSets) {
        Map<String, HashSet<String>> toReturn = new HashMap<>();
        for (String set: flagSets) {
            HashSet<String> keys = _flagSets.get(set);
            toReturn.put(set, keys);
        }
        return toReturn;
    }

    @Override
    public void kill(String splitName, String defaultTreatment, long changeNumber) {
        ParsedSplit parsedSplit = _concurrentMap.get(splitName);

        ParsedSplit updatedSplit = new ParsedSplit(parsedSplit.feature(),
                parsedSplit.seed(),
                true,
                defaultTreatment,
                parsedSplit.parsedConditions(),
                parsedSplit.trafficTypeName(),
                changeNumber,
                parsedSplit.trafficAllocation(),
                parsedSplit.trafficAllocationSeed(),
                parsedSplit.algo(),
                parsedSplit.configurations(),
                parsedSplit.flagSets(),
                parsedSplit.trackImpression()
                );

        _concurrentMap.put(splitName, updatedSplit);
    }

    @Override
    public void clear() {
        _concurrentMap.clear();
        _concurrentTrafficTypeNameSet.clear();
    }

    @Override
    public void putMany(List<ParsedSplit> splits) {
        for (ParsedSplit split : splits) {
            _concurrentMap.put(split.feature(), split);
            if (split.trafficTypeName() != null) {
                this.increaseTrafficType(split.trafficTypeName());
            }
            removeFromFlagSets(split.feature());
            addToFlagSets(split);
        }
    }

    @Override
    public void increaseTrafficType(String trafficType) {
        _concurrentTrafficTypeNameSet.add(trafficType);
    }

    @Override
    public void decreaseTrafficType(String trafficType) {
        _concurrentTrafficTypeNameSet.remove(trafficType);
    }

    @Override
    public void update(List<ParsedSplit> toAdd, List<String> toRemove, long changeNumber) {
        if(toAdd != null) {
            putMany(toAdd);
        }
        if(toRemove != null) {
            for(String featureFlag : toRemove) {
                remove(featureFlag);
            }
        }
        setChangeNumber(changeNumber);
    }

    public Set<String> getSegments() {
        return _concurrentMap.values().stream()
                .flatMap(parsedSplit -> parsedSplit.getSegmentsNames().stream()).collect(Collectors.toSet());
    }

    private void addToFlagSets(ParsedSplit featureFlag) {
        HashSet<String> sets = featureFlag.flagSets();
        if(sets == null) {
            return;
        }
        for (String set: sets) {
            if (!_flagSetsFilter.intersect(set)) {
                continue;
            }
            HashSet<String> features = _flagSets.get(set);
            if (features == null) {
                features = new HashSet<>();
            }
            features.add(featureFlag.feature());
            _flagSets.put(set, features);
        }
    }

    private void removeFromFlagSets(String featureFlagName) {
        for (String set: _flagSets.keySet()) {
            _flagSets.get(set).remove(featureFlagName);
        }
    }
}