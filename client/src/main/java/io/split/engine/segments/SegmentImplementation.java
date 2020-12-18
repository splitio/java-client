package io.split.engine.segments;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Segment Implementation
 * @author lucasecheverz
 */
import static com.google.common.base.Preconditions.checkNotNull;

public class SegmentImplementation implements Segment{

    private final String _segmentName;
    private final AtomicLong _changeNumber;
    private Set<String> _concurrentKeySet = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    public SegmentImplementation(long changeNumber, String segmentName){
        _segmentName = segmentName;
        _changeNumber = new AtomicLong(changeNumber);

        checkNotNull(_segmentName);
    }

    public SegmentImplementation(long changeNumber, String segmentName, List<String> keys){
        this(changeNumber,segmentName);
        _concurrentKeySet.addAll(keys);
    }

    @Override
    public String segmentName() {
        return _segmentName;
    }

    @Override
    public boolean contains(String key) {
        return _concurrentKeySet.contains(key);
    }

    @Override
    public void forceRefresh() {
        return;
    }

    @Override
    public long changeNumber() {
        return _changeNumber.get();
    }

    public void setChangeNumber(long changeNumber){
        _changeNumber.set(changeNumber);
    }

    public void updateSegment(List<String> toAdd, List<String> toRemove){
        _concurrentKeySet.removeAll(toRemove);
        _concurrentKeySet.addAll(toAdd);
    }
}
