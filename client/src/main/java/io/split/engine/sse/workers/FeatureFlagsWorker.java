package io.split.engine.sse.workers;

import io.split.engine.sse.dtos.FeatureFlagChangeNotification;
import io.split.engine.sse.dtos.SplitKillNotification;

public interface FeatureFlagsWorker {
    void addToQueue(FeatureFlagChangeNotification featureFlagChangeNotification);
    void start();
    void stop();
    void kill(SplitKillNotification splitKillNotification);
}