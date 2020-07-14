package io.split.engine.sse.dtos;

import io.split.engine.sse.NotificationManagerKeeper;

public interface PresenceNotification {
    void handlerPresence(NotificationManagerKeeper notificationManagerKeeper);
}
