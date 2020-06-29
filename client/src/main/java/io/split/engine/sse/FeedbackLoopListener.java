package io.split.engine.sse;

import io.split.engine.sse.dtos.ErrorNotification;
import io.split.engine.sse.dtos.IncomingNotification;

public interface FeedbackLoopListener {
    void onMessageNotificationAdded(IncomingNotification incomingNotification);
    void onErrorNotificationAdded(ErrorNotification errorNotification);
    void onConnected();
    void onDisconnect();
}
