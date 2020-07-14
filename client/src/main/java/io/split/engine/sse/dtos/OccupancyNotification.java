package io.split.engine.sse.dtos;

import io.split.engine.sse.NotificationManagerKeeper;
import io.split.engine.sse.NotificationProcessor;

public class OccupancyNotification extends IncomingNotification implements PresenceNotification {
    private final OccupancyMetrics metrics;

    public OccupancyNotification(GenericNotificationData genericNotificationData) {
        super(Type.OCCUPANCY, genericNotificationData.getChannel());
        this.metrics = genericNotificationData.getMetrics();
    }

    public OccupancyMetrics getMetrics() {
        return metrics;
    }

    @Override
    public void handler(NotificationProcessor notificationProcessor) {
        notificationProcessor.processPresence(this);
    }

    @Override
    public void handlerPresence(NotificationManagerKeeper notificationManagerKeeper) {
        if (getChannel() == "control_pri") {
            notificationManagerKeeper.handleIncomingOccupancyEvent(this);
        }
    }
}
