package io.split.storages.memory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import io.split.engine.segments.SegmentImp;
import io.split.storages.SegmentCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ConcurrentMap;

/**
 * InMemoryCache Implementation
 * @author lucasecheverz
 */
public class SegmentCacheInMemoryImpl implements SegmentCache {
    private static final Logger _log = LoggerFactory.getLogger(SegmentCacheInMemoryImpl.class);
    private static final long DEFAULT_CHANGE_NUMBER = -1l;
    private final ConcurrentMap<String, SegmentImp> _segments = Maps.newConcurrentMap();

    @Override
    public void updateSegment(String segmentName, List<String> toAdd, List<String> toRemove, long changeNumber) {
        _segments.putIfAbsent(segmentName, new SegmentImp(changeNumber, segmentName,toAdd));
        _segments.get(segmentName).update(toAdd,toRemove, changeNumber);
    }

    @Override
    public boolean isInSegment(String segmentName, String key) {
        SegmentImp segmentImp = _segments.get(segmentName);
        if(segmentImp == null){
            _log.error("Segment " + segmentName + "Not found.");
            return false;
        }
        return segmentImp.contains(key);
    }

    @Override
    public void setChangeNumber(String segmentName, long changeNumber) {
        if(_segments.get(segmentName) == null){
            _log.error("Segment " + segmentName + "Not found.");
            return ;
        }
        _segments.get(segmentName).setChangeNumber(changeNumber);
    }

    @Override
    public long getChangeNumber(String segmentName) {
        SegmentImp segmentImp = _segments.get(segmentName);
        if(segmentImp == null){
            return DEFAULT_CHANGE_NUMBER;
        }
        return segmentImp.getChangeNumber();
    }

    @VisibleForTesting
    void clear() {
        _segments.clear();
    }

    @Override
    public long getSegmentCount() {
        return _segments.values().size();
    }

    @Override
    public long getKeyCount() {
        return _segments.values().stream().mapToLong(SegmentImp::getKeysSize).sum();
    }
}
