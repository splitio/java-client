package io.split.engine.cache;

import com.google.common.collect.Maps;
import io.split.engine.experiments.ParsedSplit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryCacheImp implements SplitCache {

    private static final Logger _log = LoggerFactory.getLogger(InMemoryCacheImp.class);

    private final ConcurrentMap<String, ParsedSplit> _concurrentMap;
    private AtomicLong _changeNumber;

    public InMemoryCacheImp(long startingChangeNumber) {
        _concurrentMap = Maps.newConcurrentMap();
        _changeNumber = new AtomicLong(startingChangeNumber);
    }

    @Override
    public void put(ParsedSplit split) {
        _concurrentMap.put(split.feature(), split);
    }

    @Override
    public void putAll(Map<String, ParsedSplit> splits) {
        _concurrentMap.putAll(splits);
    }

    @Override
    public boolean remove(String name) {
        ParsedSplit removed = _concurrentMap.remove(name);

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
    public boolean trafficTypeExists(String trafficType) {
        return false;
    }

    @Override
    public void clear() {
        _concurrentMap.clear();
    }
}
