package io.split.engine.sse.listeners;

import io.split.engine.sse.dtos.ErrorNotification;

public interface FeedbackLoopListener {
    void onErrorNotificationAdded(ErrorNotification errorNotification);
    void onConnected();
    void onDisconnect();
}
