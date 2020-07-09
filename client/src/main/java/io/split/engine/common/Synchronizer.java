package io.split.engine.common;

public interface Synchronizer {
    void syncAll();
    void synchronizeSplits();
    void synchronizeSegment(String segmentName);
    void startPeriodicFetching();
    void stopPeriodicFetching();
}
