package io.split.engine.sse.dtos;

public class OccupancyMetrics {
    private final int publishers;

    public OccupancyMetrics(int publishers) {
        this.publishers = publishers;
    }

    public int getPublishers() {
        return publishers;
    }
}
