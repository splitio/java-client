package io.split.storages.pluggable.adapters;

import io.split.client.utils.SDKMetadata;
import io.split.storages.pluggable.domain.PrefixAdapter;
import io.split.storages.pluggable.domain.SafeUserStorageWrapper;
import io.split.telemetry.domain.StreamingEvent;
import io.split.telemetry.domain.enums.EventsDataRecordsEnum;
import io.split.telemetry.domain.enums.HTTPLatenciesEnum;
import io.split.telemetry.domain.enums.ImpressionsDataTypeEnum;
import io.split.telemetry.domain.enums.LastSynchronizationRecordsEnum;
import io.split.telemetry.domain.enums.MethodEnum;
import io.split.telemetry.domain.enums.ResourceEnum;
import io.split.telemetry.storage.TelemetryStorageProducer;
import io.split.telemetry.utils.BucketCalculator;
import pluggable.CustomStorageWrapper;

import static com.google.common.base.Preconditions.checkNotNull;

public class UserCustomTelemetryAdapterProducer implements TelemetryStorageProducer {

    private final SafeUserStorageWrapper _safeUserStorageWrapper;
    private SDKMetadata _sdkMetadata;

    public UserCustomTelemetryAdapterProducer(CustomStorageWrapper customStorageWrapper, SDKMetadata sdkMetadata) {
        _safeUserStorageWrapper = new SafeUserStorageWrapper(checkNotNull(customStorageWrapper));
        _sdkMetadata = sdkMetadata;
    }

    @Override
    public void recordNonReadyUsage() {
        //No-op
    }

    @Override
    public void recordBURTimeout() {
        //No-op
    }

    @Override
    public void recordLatency(MethodEnum method, long latency) {
        _safeUserStorageWrapper.increment(PrefixAdapter.buildTelemetryLatenciesPrefix(method.getMethod(), BucketCalculator.getBucketForLatency(latency), _sdkMetadata.getSdkVersion(), _sdkMetadata.getMachineIp(), _sdkMetadata.getMachineName()), 1);
    }

    @Override
    public void recordException(MethodEnum method) {
        _safeUserStorageWrapper.increment(PrefixAdapter.buildTelemetryExceptionsPrefix(method.getMethod(), _sdkMetadata.getSdkVersion(), _sdkMetadata.getMachineIp(), _sdkMetadata.getMachineName()), 1);
    }

    @Override
    public void addTag(String tag) {
        //No-op
    }

    @Override
    public void recordImpressionStats(ImpressionsDataTypeEnum dataType, long count) {
        //No-op
    }

    @Override
    public void recordEventStats(EventsDataRecordsEnum dataType, long count) {
        //No-op
    }

    @Override
    public void recordSuccessfulSync(LastSynchronizationRecordsEnum resource, long time) {
        //No-op
    }

    @Override
    public void recordSyncError(ResourceEnum resource, int status) {
        //No-op
    }

    @Override
    public void recordSyncLatency(HTTPLatenciesEnum resource, long latency) {
        //No-op
    }

    @Override
    public void recordAuthRejections() {
        //No-op
    }

    @Override
    public void recordTokenRefreshes() {
        //No-op
    }

    @Override
    public void recordStreamingEvents(StreamingEvent streamingEvent) {
        //No-op
    }

    @Override
    public void recordSessionLength(long sessionLength) {
        //No-op
    }
}
