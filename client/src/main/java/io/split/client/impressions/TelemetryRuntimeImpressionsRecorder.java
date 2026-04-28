package io.split.client.impressions;

import io.split.telemetry.domain.enums.ImpressionsDataTypeEnum;
import io.split.telemetry.storage.TelemetryRuntimeProducer;

public class TelemetryRuntimeImpressionsRecorder implements ImpressionsTelemetryRecorder {

    private final TelemetryRuntimeProducer _telemetryRuntimeProducer;

    public TelemetryRuntimeImpressionsRecorder(TelemetryRuntimeProducer telemetryRuntimeProducer) {
        _telemetryRuntimeProducer = telemetryRuntimeProducer;
    }

    @Override
    public void recordImpressionsDropped(long count) {
        _telemetryRuntimeProducer.recordImpressionStats(ImpressionsDataTypeEnum.IMPRESSIONS_DROPPED, count);
    }

    @Override
    public void recordImpressionsQueued(long count) {
        _telemetryRuntimeProducer.recordImpressionStats(ImpressionsDataTypeEnum.IMPRESSIONS_QUEUED, count);
    }
}
