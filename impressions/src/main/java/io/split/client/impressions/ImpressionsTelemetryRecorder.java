package io.split.client.impressions;

public interface ImpressionsTelemetryRecorder {
    void recordImpressionsDropped(long count);
    void recordImpressionsQueued(long count);
}
