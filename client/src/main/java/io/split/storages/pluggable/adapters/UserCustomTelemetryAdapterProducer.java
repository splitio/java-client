package io.split.storages.pluggable.adapters;

import io.split.client.utils.Json;
import io.split.client.utils.SDKMetadata;
import io.split.storages.pluggable.CustomStorageWrapper;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

import static com.google.common.base.Preconditions.checkNotNull;

public class UserCustomTelemetryAdapterProducer implements TelemetryStorageProducer {

    private static final Logger _log = LoggerFactory.getLogger(UserCustomTelemetryAdapterProducer.class);
    private static final String TOKEN_REFRESH = "tokenRefresh";
    private static final String NON_READY_USAGES = "nonReadyUsages";
    private static final String BUR_TIMEOUT = "burTimeout";
    private static final String TAG = "tag";
    private static final String AUTH_REJECTIONS = "authRejections";
    private static final String STREAMING_EVENT = "streamingEvent";
    private static final String SESSION = "sessionLength";

    private final SafeUserStorageWrapper _safeUserStorageWrapper;
    private SDKMetadata _sdkMetadata;

    public UserCustomTelemetryAdapterProducer(CustomStorageWrapper customStorageWrapper, SDKMetadata sdkMetadata) {
        _safeUserStorageWrapper = new SafeUserStorageWrapper(checkNotNull(customStorageWrapper));
        _sdkMetadata = sdkMetadata;
    }

    @Override
    public void recordNonReadyUsage() {
        _safeUserStorageWrapper.increment(PrefixAdapter.buildTelemetryPrefix(NON_READY_USAGES), 1L);
    }

    @Override
    public void recordBURTimeout() {
        _safeUserStorageWrapper.increment(PrefixAdapter.buildTelemetryPrefix(BUR_TIMEOUT), 1L);

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
        _safeUserStorageWrapper.pushItems(PrefixAdapter.buildTelemetryPrefix(TAG), Collections.singletonList(tag));
    }

    @Override
    public void recordImpressionStats(ImpressionsDataTypeEnum dataType, long count) {
        _safeUserStorageWrapper.increment(PrefixAdapter.buildTelemetryPrefix(dataType.toString()), count);
    }

    @Override
    public void recordEventStats(EventsDataRecordsEnum dataType, long count) {
        _safeUserStorageWrapper.increment(PrefixAdapter.buildTelemetryPrefix(dataType.toString()), count);
    }

    @Override
    public void recordSuccessfulSync(LastSynchronizationRecordsEnum resource, long time) {
        _safeUserStorageWrapper.set(PrefixAdapter.buildTelemetryPrefix(resource.toString()), Json.toJson(time));
    }

    @Override
    public void recordSyncError(ResourceEnum resource, int status) {
        _safeUserStorageWrapper.increment(PrefixAdapter.buildTelemetryPrefix(String.format(resource.toString() + ".%s", status)), 1);
    }

    @Override
    public void recordSyncLatency(HTTPLatenciesEnum resource, long latency) {
        _safeUserStorageWrapper.increment(PrefixAdapter.buildTelemetryPrefix(String.format(resource.toString() + ".%s", BucketCalculator.getBucketForLatency(latency))), 1);
    }

    @Override
    public void recordAuthRejections() {
        _safeUserStorageWrapper.increment(PrefixAdapter.buildTelemetryPrefix(AUTH_REJECTIONS), 1);
    }

    @Override
    public void recordTokenRefreshes() {
        _safeUserStorageWrapper.increment(PrefixAdapter.buildTelemetryPrefix(TOKEN_REFRESH), 1);
    }

    @Override
    public void recordStreamingEvents(StreamingEvent streamingEvent) {
        _safeUserStorageWrapper.pushItems(PrefixAdapter.buildTelemetryPrefix(STREAMING_EVENT), Collections.singletonList(Json.toJson(streamingEvent)));
    }

    @Override
    public void recordSessionLength(long sessionLength) {
        _safeUserStorageWrapper.set(PrefixAdapter.buildTelemetryPrefix(SESSION), Json.toJson(sessionLength));
    }
}
