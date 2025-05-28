package io.split.engine.sse.workers;

import io.split.engine.sse.dtos.IncomingNotification;
import io.split.engine.sse.dtos.SplitKillNotification;

public interface FeatureFlagsWorker {
    void addToQueue(IncomingNotification incomingNotification);
    void start();
    void stop();
    void kill(SplitKillNotification splitKillNotification);
}