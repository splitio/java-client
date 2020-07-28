package io.split.engine.sse.dtos;

import io.split.engine.sse.NotificationManagerKeeper;
import io.split.engine.sse.NotificationProcessor;

public class OccupancyNotification extends IncomingNotification implements StatusNotification {
    private static final String CONTROL_PRI_CHANNEL = "control_pri";
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
    public void handlerStatus(NotificationManagerKeeper notificationManagerKeeper) {
        if (CONTROL_PRI_CHANNEL.equals(getChannel())) {
            notificationManagerKeeper.handleIncomingOccupancyEvent(this);
        }
    }
}
