package io.split.engine.sse;

import io.split.engine.sse.dtos.IncomingNotification;

public interface EventSourceClient {
    void resetUrl(String url);
    void stop();

    void registerListener(FeedbackLoopListener listener);
    void notifyIncomingNotification (IncomingNotification incomingNotification);
    void notifyConnected();
    void notifyDisconnect();
}
