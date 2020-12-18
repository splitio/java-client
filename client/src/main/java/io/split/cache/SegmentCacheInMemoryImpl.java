package io.split.cache;

import com.google.common.collect.Maps;
import io.split.engine.segments.SegmentImplementation;
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
    private final ConcurrentMap<String, SegmentImplementation> _segmentFetchers = Maps.newConcurrentMap();

    public SegmentCacheInMemoryImpl(){};

    @Override
    public void updateSegment(String segmentName, List<String> toAdd, List<String> toRemove) {
        if(_segmentFetchers.get(segmentName) == null){
            _segmentFetchers.put(segmentName, new SegmentImplementation(DEFAULT_CHANGE_NUMBER, segmentName,toAdd));
        }

        _segmentFetchers.get(segmentName).updateSegment(toAdd,toRemove);
    }

    @Override
    public boolean isInSegment(String segmentName, String key) {
        if(_segmentFetchers.get(segmentName) == null){
            _log.error("Segment " + segmentName + "Not founded.");
            return false;
        }
        return _segmentFetchers.get(segmentName).contains(key);
    }

    @Override
    public void setChangeNumber(String segmentName, long changeNumber) {
        if(_segmentFetchers.get(segmentName) != null){
            _segmentFetchers.get(segmentName).setChangeNumber(changeNumber);
        }
        else{
            _log.error("Segment " + segmentName + "Not founded.");
        }
    }

    @Override
    public long getChangeNumber(String segmentName) {
        if(_segmentFetchers.get(segmentName) == null){
            _log.error("Segment " + segmentName + "Not founded.");
            return DEFAULT_CHANGE_NUMBER;
        }
        return _segmentFetchers.get(segmentName).changeNumber();
    }

    @Override
    public void clear() {
        _segmentFetchers.clear();
    }
}
