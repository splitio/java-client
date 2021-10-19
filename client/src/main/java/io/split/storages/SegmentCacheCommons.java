package io.split.storages;

public interface SegmentCacheCommons {

    /**
     * returns the changeNumber of a segment
     * @param segmentName
     * @return
     */
    long getChangeNumber(String segmentName);
}
