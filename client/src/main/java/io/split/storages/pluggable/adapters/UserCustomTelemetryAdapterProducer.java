package io.split.storages.pluggable.adapters;

import io.split.storages.pluggable.CustomStorageWrapper;
import io.split.storages.pluggable.domain.SafeUserStorageWrapper;
import io.split.telemetry.domain.StreamingEvent;
import io.split.telemetry.domain.enums.EventsDataRecordsEnum;
import io.split.telemetry.domain.enums.HTTPLatenciesEnum;
import io.split.telemetry.domain.enums.ImpressionsDataTypeEnum;
import io.split.telemetry.domain.enums.LastSynchronizationRecordsEnum;
import io.split.telemetry.domain.enums.MethodEnum;
import io.split.telemetry.domain.enums.ResourceEnum;
import io.split.telemetry.storage.TelemetryStorageProducer;

import static com.google.common.base.Preconditions.checkNotNull;

public class UserCustomTelemetryAdapterProducer implements TelemetryStorageProducer {

    private final SafeUserStorageWrapper _safeUserStorageWrapper;

    public UserCustomTelemetryAdapterProducer(CustomStorageWrapper customStorageWrapper) {
        _safeUserStorageWrapper = new SafeUserStorageWrapper(checkNotNull(customStorageWrapper));
    }

    @Override
    public void recordNonReadyUsage() {
        _safeUserStorageWrapper.increment("", 1L);
    }

    @Override
    public void recordBURTimeout() {

    }

    @Override
    public void recordLatency(MethodEnum method, long latency) {

    }

    @Override
    public void recordException(MethodEnum method) {

    }

    @Override
    public void addTag(String tag) {

    }

    @Override
    public void recordImpressionStats(ImpressionsDataTypeEnum dataType, long count) {

    }

    @Override
    public void recordEventStats(EventsDataRecordsEnum dataType, long count) {

    }

    @Override
    public void recordSuccessfulSync(LastSynchronizationRecordsEnum resource, long time) {

    }

    @Override
    public void recordSyncError(ResourceEnum resource, int status) {

    }

    @Override
    public void recordSyncLatency(HTTPLatenciesEnum resource, long latency) {

    }

    @Override
    public void recordAuthRejections() {

    }

    @Override
    public void recordTokenRefreshes() {

    }

    @Override
    public void recordStreamingEvents(StreamingEvent streamingEvent) {

    }

    @Override
    public void recordSessionLength(long sessionLength) {

    }
}
