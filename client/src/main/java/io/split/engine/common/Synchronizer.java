package io.split.engine.common;

public interface Synchronizer {
    boolean syncAll();
    void startPeriodicFetching();
    void stopPeriodicFetching();
    void refreshSplits(Long targetChangeNumber);
    void localKillSplit(String featureFlagName, String defaultTreatment, long newChangeNumber);
    void refreshSegment(String segmentName, Long targetChangeNumber);
    void startPeriodicDataRecording();
    void stopPeriodicDataRecording();
}
