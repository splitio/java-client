package io.split.engine.sse;

import io.split.engine.sse.dtos.ErrorNotification;
import io.split.engine.sse.dtos.IncomingNotification;

public interface EventSourceClient {
    void resetUrl(String url);
    void stop();

    void registerListener(FeedbackLoopListener listener);
    void notifyMessageNotification (IncomingNotification incomingNotification);
    void notifyErrorNotification (ErrorNotification errorNotification);
    void notifyConnected();
    void notifyDisconnect();
}
