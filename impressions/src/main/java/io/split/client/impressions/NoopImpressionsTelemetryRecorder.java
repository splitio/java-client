package io.split.client.impressions;

public class NoopImpressionsTelemetryRecorder implements ImpressionsTelemetryRecorder {
    @Override
    public void recordImpressionsDropped(long count) {}

    @Override
    public void recordImpressionsQueued(long count) {}
}
