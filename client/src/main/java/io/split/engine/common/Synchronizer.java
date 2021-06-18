package io.split.engine.common;

public interface Synchronizer {
    boolean syncAll();
    void startPeriodicFetching();
    void stopPeriodicFetching();
    void refreshSplits(long targetChangeNumber);
    void localKillSplit(String splitName, String defaultTreatment, long newChangeNumber);
    void refreshSegment(String segmentName, long targetChangeNumber);
}
