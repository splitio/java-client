package io.split.engine.sse.listeners;

import io.split.engine.sse.dtos.IncomingNotification;

public interface NotificationsListener {
    void onMessageNotificationReceived(IncomingNotification incomingNotification);
}
