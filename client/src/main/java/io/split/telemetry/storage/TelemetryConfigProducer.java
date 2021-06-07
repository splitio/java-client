package io.split.telemetry.storage;

public interface TelemetryConfigProducer {
    void recordNonReadyUsage();
    void recordBURTimeout();
}
