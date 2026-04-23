package io.split.client.events;

import io.split.telemetry.domain.enums.EventsDataRecordsEnum;
import io.split.telemetry.storage.TelemetryRuntimeProducer;

import java.util.Objects;

public class TelemetryEventQueueStats implements EventQueueStats {

    private final TelemetryRuntimeProducer _telemetryRuntimeProducer;

    public TelemetryEventQueueStats(TelemetryRuntimeProducer telemetryRuntimeProducer) {
        _telemetryRuntimeProducer = Objects.requireNonNull(telemetryRuntimeProducer);
    }

    @Override
    public void onQueued(long count) {
        _telemetryRuntimeProducer.recordEventStats(EventsDataRecordsEnum.EVENTS_QUEUED, count);
    }

    @Override
    public void onDropped(long count) {
        _telemetryRuntimeProducer.recordEventStats(EventsDataRecordsEnum.EVENTS_DROPPED, count);
    }
}
