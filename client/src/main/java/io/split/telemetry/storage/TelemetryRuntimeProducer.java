package io.split.telemetry.storage;

import io.split.telemetry.domain.StreamingEvent;
import io.split.telemetry.domain.enums.EventsDataRecordsEnum;
import io.split.telemetry.domain.enums.ImpressionsDataTypeEnum;
import io.split.telemetry.domain.enums.LastSynchronizationRecordsEnum;
import io.split.telemetry.domain.enums.ResourceEnum;

public interface TelemetryRuntimeProducer {
    void addTag(String tag);
    void recordImpressionStats(ImpressionsDataTypeEnum dataType, long count);
    void recordEventStats(EventsDataRecordsEnum dataType, long count);
    void recordSuccessfulSync(LastSynchronizationRecordsEnum resource, long time);
    void recordSyncError(ResourceEnum resource, int status);
    void recordSyncLatency(String resource, long latency);
    void recordAuthRejections();
    void recordTokenRefreshes();
    void recordStreamingEvents(StreamingEvent streamingEvent);
    void recordSessionLength(long sessionLength);
}
