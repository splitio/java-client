package io.split.engine.common;

public interface Synchronizer {
    boolean syncAll();
    void startPeriodicFetching();
    void stopPeriodicFetching();
    void refreshSplits(Long targetChangeNumber);
    void localKillSplit(String splitName, String defaultTreatment, long newChangeNumber);
    void refreshSegment(String segmentName, Long targetChangeNumber);
    void startPeriodicDataRecording();
    void stopPeriodicDataRecording(long splitCount, long segmentCount, long segmentKeyCount);
}
