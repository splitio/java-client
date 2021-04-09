package io.split.telemetry.storage;

import io.split.telemetry.domain.enums.MethodEnum;

public interface TelemetryEvaluationProducer {
    void recordLatency(String method, int latency);
    void recordException(MethodEnum method);
}
