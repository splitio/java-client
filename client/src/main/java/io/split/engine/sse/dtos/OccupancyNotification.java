package io.split.engine.sse.dtos;

public class OccupancyNotification extends IncomingNotification {
    private OccupancyMetrics metrics;

    public OccupancyMetrics getMetrics() {
        return metrics;
    }

    public void setMetrics(OccupancyMetrics metrics) {
        this.metrics = metrics;
    }
}
