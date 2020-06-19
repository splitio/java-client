package io.split.engine.sse;

import io.split.engine.sse.dtos.IncomingNotification;

public interface FeedbackLoopListener {
    void onIncomingNotificationAdded(IncomingNotification incomingNotification);
    void onConnected();
    void onDisconnect();
}
