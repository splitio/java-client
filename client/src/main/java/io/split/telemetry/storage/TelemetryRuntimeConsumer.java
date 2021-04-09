package io.split.telemetry.storage;

import io.split.telemetry.domain.HTTPErrors;
import io.split.telemetry.domain.HTTPLatencies;
import io.split.telemetry.domain.LastSynchronization;
import io.split.telemetry.domain.StreamingEvent;
import io.split.telemetry.domain.enums.EventsDataRecordsEnum;
import io.split.telemetry.domain.enums.ImpressionsDataTypeEnum;

import java.util.List;

public interface TelemetryRuntimeConsumer {
    long getImpressionsStats(ImpressionsDataTypeEnum data);
    long getEventStats(EventsDataRecordsEnum type);
    LastSynchronization getLastSynchronization();
    HTTPErrors popHTTPErrors();
    HTTPLatencies popHTTPLatencies() throws Exception;
    long popAuthRejections();
    long popTokenRefreshes();
    List<StreamingEvent> popStreamingEvents();
    List<String> popTags();
    long getSessionLength();

}
