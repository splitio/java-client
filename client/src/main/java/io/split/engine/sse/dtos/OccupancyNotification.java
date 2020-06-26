package io.split.engine.sse.dtos;

public class OccupancyNotification extends IncomingNotification {
    private final OccupancyMetrics metrics;

    public OccupancyNotification(GenericNotificationData genericNotificationData) {
        super(Type.OCCUPANCY, genericNotificationData.getChannel());
        this.metrics = genericNotificationData.getMetrics();
    }

    public OccupancyMetrics getMetrics() {
        return metrics;
    }
}
