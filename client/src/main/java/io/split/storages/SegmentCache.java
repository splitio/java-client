package io.split.storages;

import io.split.engine.segments.SegmentImp;

import java.util.List;

/**
 * Memory for segments
 * @author lucasecheverz
 */
public interface SegmentCache {

    /**
     * update segment
     * @param segmentName
     * @param toAdd
     * @param toRemove
     */
    void updateSegment(String segmentName, List<String> toAdd, List<String> toRemove) ;

    /**
     * evaluates if a key belongs to a segment
     * @param segmentName
     * @param key
     * @return
     */
    boolean isInSegment(String segmentName, String key);

    /**
     * update the changeNumber of a segment
     * @param segmentName
     * @param changeNumber
     */
    void setChangeNumber(String segmentName, long changeNumber);

    /**
     * returns the changeNumber of a segment
     * @param segmentName
     * @return
     */
    long getChangeNumber(String segmentName);

    /**
     * clear all segments
     */
    void clear();

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
