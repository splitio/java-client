package io.split.engine.segments;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class SegmentImpMauro {
    private final String _name;
    private final AtomicLong _changeNumber;
    private Set<String> _concurrentKeySet = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public SegmentImpMauro(String name, long changeNumber) {
        _name = name;
        _changeNumber = new AtomicLong(changeNumber);
    }

    public SegmentImpMauro(long changeNumber, String name, List<String> keys){
        this(name, changeNumber);
        _concurrentKeySet.addAll(keys);
    }

    public String getName() {
        return _name;
    }

    public long getChangeNumber() {
        return _changeNumber.get();
    }

    public void setChangeNumber(long changeNumber){
        _changeNumber.set(changeNumber);
    }

    public void update(List<String> toAdd, List<String> toRemove){
        _concurrentKeySet.removeAll(toRemove);
        _concurrentKeySet.addAll(toAdd);
    }

    public boolean contains(String key) {
        return _concurrentKeySet.contains(key);
    }
}
