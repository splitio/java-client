package io.split.engine.segments;

public interface TelemetryListener {
    void recordSuccessfulSync(long time);
}
