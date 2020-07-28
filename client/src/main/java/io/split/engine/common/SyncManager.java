package io.split.engine.common;

import io.split.engine.sse.listeners.FeedbackLoopListener;
import io.split.engine.sse.listeners.NotificationKeeperListener;

public interface SyncManager extends NotificationKeeperListener, FeedbackLoopListener {
    void start();
    void shutdown();
}
