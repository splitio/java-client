package io.split.engine.segments;

import io.split.engine.experiments.SyncTask;

public interface SegmentSynchronizationTask extends SyncTask {
    /**
     * initializes the segment
     * @param segmentName
     */
    void initializeSegment(String segmentName);

    /**
     * returns segmentFecther
     * @param segmentName
     * @return
     */
    SegmentFetcher getFetcher(String segmentName);

    /**
     * starts the fetching
     */
    void start();

    /**
     * stops the thread
     */
    void stop();

    /**
     * fetch every Segment
     * @param addCacheHeader
     */
    void fetchAll(boolean addCacheHeader);

    /**
     * fetch every Segment Synchronous
     */
    boolean fetchAllSynchronous();
    void close();
}
