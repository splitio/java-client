package io.split.engine.segments;

/**
 * Created by adilaijaz on 5/7/15.
 */
public interface SegmentFetcher {
    Segment segment(String segmentName);
    long getChangeNumber(String segmentName);
    void forceRefresh(String segmentName);
    void forceRefreshAll();
    void startPeriodicFetching();
}
