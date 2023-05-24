package io.split.engine.common;

import io.split.engine.sse.dtos.FeatureFlagChangeNotification;
import io.split.engine.sse.dtos.SplitKillNotification;

public interface Synchronizer {
    boolean syncAll();
    void startPeriodicFetching();
    void stopPeriodicFetching();
    void refreshSplits(FeatureFlagChangeNotification featureFlagChangeNotification);
    void localKillSplit(SplitKillNotification splitKillNotification);
    void refreshSegment(String segmentName, Long targetChangeNumber);
    void startPeriodicDataRecording();
    void stopPeriodicDataRecording();
}
