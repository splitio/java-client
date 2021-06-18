package io.split.telemetry.storage;

import io.split.telemetry.domain.enums.MethodEnum;

public interface TelemetryEvaluationProducer {
    void recordLatency(MethodEnum method, long latency);
    void recordException(MethodEnum method);
}
