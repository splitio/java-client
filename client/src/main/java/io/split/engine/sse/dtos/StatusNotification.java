package io.split.engine.sse.dtos;

import io.split.engine.sse.NotificationManagerKeeper;

public interface StatusNotification {
    void handlerStatus(NotificationManagerKeeper notificationManagerKeeper);
}
