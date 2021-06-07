package io.split.telemetry.storage;

public interface TelemetryConfigConsumer {
    long getBURTimeouts();
    long getNonReadyUsages();
}
