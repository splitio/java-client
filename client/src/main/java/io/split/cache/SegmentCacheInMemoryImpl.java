package io.split.cache;

import com.google.common.collect.Maps;
import io.split.engine.segments.SegmentImp;
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

    public SegmentCacheInMemoryImpl(){};

    @Override
    public void updateSegment(String segmentName, List<String> toAdd, List<String> toRemove) {
        if(_segments.get(segmentName) == null){
            _segments.put(segmentName, new SegmentImp(DEFAULT_CHANGE_NUMBER, segmentName,toAdd));
        }

        _segments.get(segmentName).update(toAdd,toRemove);
    }

    @Override
    public boolean isInSegment(String segmentName, String key) {
        if(_segments.get(segmentName) == null){
            _log.error("Segment " + segmentName + "Not founded.");
            return false;
        }
        return _segments.get(segmentName).contains(key);
    }

    @Override
    public void setChangeNumber(String segmentName, long changeNumber) {
        if(_segments.get(segmentName) != null){
            _segments.get(segmentName).setChangeNumber(changeNumber);
        }
        else{
            _log.error("Segment " + segmentName + "Not founded.");
        }
    }

    @Override
    public long getChangeNumber(String segmentName) {
        if(_segments.get(segmentName) == null){
            _log.error("Segment " + segmentName + "Not founded.");
            return DEFAULT_CHANGE_NUMBER;
        }
        return _segments.get(segmentName).getChangeNumber();
    }

    @Override
    public void clear() {
        _segments.clear();
    }
}
