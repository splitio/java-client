package io.split.storages;

import io.split.engine.segments.SegmentImp;

import java.util.List;

public interface SegmentCacheConsumer extends SegmentCacheCommons{
    /**
     * evaluates if a key belongs to a segment
     * @param segmentName
     * @param key
     * @return
     */
    boolean isInSegment(String segmentName, String key);

    /**
     * return every segment
     * @return
     */
    List<SegmentImp> getAll();

    /**
     * return key count
     * @return
     */
    long getKeyCount();
}
