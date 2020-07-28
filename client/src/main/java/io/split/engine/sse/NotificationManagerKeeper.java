package io.split.engine.sse;

import io.split.engine.sse.dtos.ControlNotification;
import io.split.engine.sse.dtos.OccupancyNotification;
import io.split.engine.sse.listeners.NotificationKeeperListener;

public interface NotificationManagerKeeper {
    void handleIncomingControlEvent(ControlNotification controlNotification);
    void handleIncomingOccupancyEvent(OccupancyNotification occupancyNotification);

    void registerNotificationKeeperListener(NotificationKeeperListener listener);
}
