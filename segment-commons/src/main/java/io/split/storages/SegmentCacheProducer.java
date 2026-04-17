package io.split.storages;

import java.util.List;

public interface SegmentCacheProducer extends SegmentCacheCommons{
    /**
     * update segment
     * @param segmentName
     * @param toAdd
     * @param toRemove
     */
    void updateSegment(String segmentName, List<String> toAdd, List<String> toRemove, long changeNumber) ;

    /**
     * update the changeNumber of a segment
     * @param segmentName
     * @param changeNumber
     */
    void setChangeNumber(String segmentName, long changeNumber);
}
