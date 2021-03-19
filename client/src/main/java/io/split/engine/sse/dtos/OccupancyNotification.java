package io.split.engine.sse.dtos;

import io.split.engine.sse.PushStatusTracker;
import io.split.engine.sse.NotificationProcessor;

public class OccupancyNotification extends IncomingNotification implements StatusNotification {
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
        notificationProcessor.processStatus(this);
    }

    @Override
    public void handlerStatus(PushStatusTracker notificationManagerKeeper) {
        notificationManagerKeeper.handleIncomingOccupancyEvent(this);
    }

    @Override
    public String toString() {
        try {
            return String.format("Type: %s; Channel: %s; Publishers: %s", getType(), getChannel(), getMetrics().getPublishers());
        } catch (Exception ex) {
            return super.toString();
        }
    }
}
