package io.split.telemetry.storage;

public interface TelemetryConfigProducer {
    void recordConfigData();
    void recordNonReadyUsage();
    void recordBURTimeout();
}
