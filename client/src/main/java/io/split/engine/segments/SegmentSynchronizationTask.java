package io.split.engine.segments;

public interface SegmentSynchronizationTask extends Runnable {
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
    void startPeriodicFetching();

    /**
     * stops the thread
     */
    void stop();
}
