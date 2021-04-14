package io.split.cache;

import com.google.common.collect.Maps;
import io.split.engine.segments.SegmentImp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * InMemoryCache Implementation
 * @author lucasecheverz
 */
public class SegmentCacheInMemoryImpl implements SegmentCache {
    private static final Logger _log = LoggerFactory.getLogger(SegmentCacheInMemoryImpl.class);
    private static final long DEFAULT_CHANGE_NUMBER = -1l;
    private final ConcurrentMap<String, SegmentImp> _segments = Maps.newConcurrentMap();

    @Override
    public void updateSegment(String segmentName, List<String> toAdd, List<String> toRemove) {
        if(_segments.get(segmentName) == null){
            _segments.put(segmentName, new SegmentImp(DEFAULT_CHANGE_NUMBER, segmentName,toAdd));
        }

        _segments.get(segmentName).update(toAdd,toRemove);
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
            _log.error("Segment " + segmentName + "Not found.");
            return DEFAULT_CHANGE_NUMBER;
        }
        return segmentImp.getChangeNumber();
    }

    @Override
    public void clear() {
        _segments.clear();
    }

    @Override
    public List<SegmentImp> getAll() {
        return _segments.values().stream().collect(Collectors.toList());
    }

    @Override
    public Set<String> getAllKeys() {
        return _segments.values().stream().flatMap(si -> si.getKeys().stream()).collect(Collectors.toSet());
    }
}
