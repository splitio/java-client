package io.split.engine.sse;

import io.split.engine.sse.listeners.FeedbackLoopListener;
import io.split.engine.sse.listeners.NotificationsListener;

public interface EventSourceClient {
    void start();
    void stop();
    void setUrl(String url);

    void registerNotificationListener(NotificationsListener listener);
    void registerFeedbackListener(FeedbackLoopListener listener);
}
