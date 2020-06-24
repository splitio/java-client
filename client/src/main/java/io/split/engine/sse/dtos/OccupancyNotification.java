package io.split.engine.sse.dtos;

public class OccupancyNotification extends IncomingNotification {
    private final OccupancyMetrics metrics;

    public OccupancyNotification(String channel, int publishers) {
        super(Type.OCCUPANCY, channel);
        this.metrics = new OccupancyMetrics(publishers);
    }

    public OccupancyMetrics getMetrics() {
        return metrics;
    }
}
