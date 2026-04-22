package io.split.client.events;

import io.split.telemetry.domain.enums.EventsDataRecordsEnum;
import io.split.telemetry.storage.TelemetryRuntimeProducer;
import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.verify;

public class TelemetryEventQueueStatsTest {

    @Test
    public void onQueuedRecordsEventStats() {
        TelemetryRuntimeProducer telemetryRuntimeProducer = Mockito.mock(TelemetryRuntimeProducer.class);
        TelemetryEventQueueStats stats = new TelemetryEventQueueStats(telemetryRuntimeProducer);

        stats.onQueued(42);

        verify(telemetryRuntimeProducer).recordEventStats(EventsDataRecordsEnum.EVENTS_QUEUED, 42);
    }

    @Test
    public void onDroppedRecordsEventStats() {
        TelemetryRuntimeProducer telemetryRuntimeProducer = Mockito.mock(TelemetryRuntimeProducer.class);
        TelemetryEventQueueStats stats = new TelemetryEventQueueStats(telemetryRuntimeProducer);

        stats.onDropped(10);

        verify(telemetryRuntimeProducer).recordEventStats(EventsDataRecordsEnum.EVENTS_DROPPED, 10);
    }

}
