package io.split.engine.cache;

import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import io.split.engine.experiments.ParsedSplit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryCacheImp implements SplitCache {

    private static final Logger _log = LoggerFactory.getLogger(InMemoryCacheImp.class);

    private final ConcurrentMap<String, ParsedSplit> _concurrentMap;
    private final Multiset<String> _concurrentTrafficTypeNameSet;

    private AtomicLong _changeNumber;

    public InMemoryCacheImp() {
        this(-1);
    }

    public InMemoryCacheImp(long startingChangeNumber) {
        _concurrentMap = Maps.newConcurrentMap();
        _changeNumber = new AtomicLong(startingChangeNumber);
        _concurrentTrafficTypeNameSet = ConcurrentHashMultiset.create();
    }

    @Override
    public void put(ParsedSplit split) {
        _concurrentMap.put(split.feature(), split);

        if (split.trafficTypeName() != null) {
            _concurrentTrafficTypeNameSet.add(split.trafficTypeName());
        }
    }

    @Override
    public boolean remove(String name) {
        ParsedSplit removed = _concurrentMap.remove(name);

        if (removed != null && removed.trafficTypeName() != null) {
            _concurrentTrafficTypeNameSet.remove(removed.trafficTypeName());
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
    public Collection<ParsedSplit> getMany(List<String> names) {
        List<ParsedSplit> splits = new ArrayList<>();

        for (String name : names) {
            ParsedSplit split = _concurrentMap.get(name);

            if (split != null) {
                splits.add(split);
            }
        }

        return splits;
    }

    @Override
    public long getChangeNumber() {
        return _changeNumber.get();
    }

    @Override
    public void setChangeNumber(long changeNumber) {
        if (changeNumber < _changeNumber.get()) {
            _log.error("ChangeNumber for splits cache is less than previous");
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
                parsedSplit.configurations());

        _concurrentMap.put(splitName, updatedSplit);
    }

    @Override
    public void clear() {
        _concurrentMap.clear();
        _concurrentTrafficTypeNameSet.clear();
    }
}
