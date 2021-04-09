package io.split.telemetry.storage;

import io.split.telemetry.domain.MethodExceptions;
import io.split.telemetry.domain.MethodLatencies;

public interface TelemetryEvaluationConsumer {
    MethodExceptions popExceptions() throws Exception;
    MethodLatencies popLatencies();
}
