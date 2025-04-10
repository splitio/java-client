package io.split.storages.memory;

import com.google.common.collect.Maps;
import io.split.engine.experiments.ParsedRuleBasedSegment;
import io.split.storages.RuleBasedSegmentCache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class RuleBasedSegmentCacheInMemoryImp implements RuleBasedSegmentCache {

    private static final Logger _log = LoggerFactory.getLogger(RuleBasedSegmentCacheInMemoryImp.class);

    private final ConcurrentMap<String, ParsedRuleBasedSegment> _concurrentMap;

    private AtomicLong _changeNumber;

    public RuleBasedSegmentCacheInMemoryImp() {
        this(-1);
    }

    public RuleBasedSegmentCacheInMemoryImp(long startingChangeNumber) {
        _concurrentMap = Maps.newConcurrentMap();
        _changeNumber = new AtomicLong(startingChangeNumber);
    }

    @Override
    public boolean remove(String name) {
        ParsedRuleBasedSegment removed = _concurrentMap.remove(name);
        return removed != null;
    }

    @Override
    public ParsedRuleBasedSegment get(String name) {
        return _concurrentMap.get(name);
    }

    @Override
    public Collection<ParsedRuleBasedSegment> getAll() {
        return _concurrentMap.values();
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
    public List<String> ruleBasedSegmentNames() {
        List<String> ruleBasedSegmentNamesList = new ArrayList<>();
        for (String key: _concurrentMap.keySet()) {
            ruleBasedSegmentNamesList.add(_concurrentMap.get(key).ruleBasedSegment());
        }
        return ruleBasedSegmentNamesList;
    }

    public void clear() {
        _concurrentMap.clear();
    }

    private void putMany(List<ParsedRuleBasedSegment> ruleBasedSegments) {
        for (ParsedRuleBasedSegment ruleBasedSegment : ruleBasedSegments) {
            _concurrentMap.put(ruleBasedSegment.ruleBasedSegment(), ruleBasedSegment);
        }
    }

    @Override
    public void update(List<ParsedRuleBasedSegment> toAdd, List<String> toRemove, long changeNumber) {
        if(toAdd != null) {
            putMany(toAdd);
        }
        if(toRemove != null) {
            for(String ruleBasedSegment : toRemove) {
                remove(ruleBasedSegment);
            }
        }
        setChangeNumber(changeNumber);
    }

    public Set<String> getSegments() {
        return _concurrentMap.values().stream()
                .flatMap(parsedRuleBasedSegment -> parsedRuleBasedSegment.getSegmentsNames().stream()).collect(Collectors.toSet());
    }
}