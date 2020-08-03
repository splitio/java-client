package io.split.engine.sse;

import io.split.engine.sse.listeners.FeedbackLoopListener;
import io.split.engine.sse.listeners.NotificationsListener;

public interface EventSourceClient {
    boolean start(String target);
    void stop();

    void registerNotificationListener(NotificationsListener listener);
    void registerFeedbackListener(FeedbackLoopListener listener);
}
