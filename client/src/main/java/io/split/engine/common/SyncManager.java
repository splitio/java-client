package io.split.engine.common;

import io.split.engine.sse.listeners.FeedbackLoopListener;
import io.split.engine.sse.listeners.NotificationKeeperListener;

public interface SyncManager {
    void start();
    void shutdown();
}
