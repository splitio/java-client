package io.split.engine.sse;

import io.split.engine.sse.dtos.ErrorNotification;
import io.split.engine.sse.dtos.IncomingNotification;
import io.split.engine.sse.listeners.FeedbackLoopListener;
import io.split.engine.sse.listeners.NotificationsListener;

public interface EventSourceClient {
    void start(String url);
    void stop();

    void registerNotificationListener(NotificationsListener listener);
    void registerFeedbackListener(FeedbackLoopListener listener);
    void notifyMessageNotification (IncomingNotification incomingNotification);
    void notifyErrorNotification (ErrorNotification errorNotification);
    void notifyConnected();
    void notifyDisconnect();
}
