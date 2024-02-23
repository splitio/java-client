package io.split.telemetry.storage;

import io.split.telemetry.domain.HTTPErrors;
import io.split.telemetry.domain.HTTPLatencies;
import io.split.telemetry.domain.LastSynchronization;
import io.split.telemetry.domain.MethodExceptions;
import io.split.telemetry.domain.MethodLatencies;
import io.split.telemetry.domain.StreamingEvent;
import io.split.telemetry.domain.UpdatesFromSSE;
import io.split.telemetry.domain.enums.EventsDataRecordsEnum;
import io.split.telemetry.domain.enums.HTTPLatenciesEnum;
import io.split.telemetry.domain.enums.ImpressionsDataTypeEnum;
import io.split.telemetry.domain.enums.LastSynchronizationRecordsEnum;
import io.split.telemetry.domain.enums.MethodEnum;
import io.split.telemetry.domain.enums.ResourceEnum;
import io.split.telemetry.domain.enums.UpdatesFromSSEEnum;

import java.util.List;

public class NoopTelemetryStorage implements TelemetryStorage{

    @Override
    public void recordNonReadyUsage() {

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

    @Override
    public void recordUpdatesFromSSE(UpdatesFromSSEEnum updatesFromSSEEnum) {

    }

    @Override
    public long getBURTimeouts() {
        return 0;
    }

    @Override
    public long getNonReadyUsages() {
        return 0;
    }

    @Override
    public MethodExceptions popExceptions() throws Exception {
        return null;
    }

    @Override
    public MethodLatencies popLatencies() throws Exception {
        return null;
    }

    @Override
    public long getImpressionsStats(ImpressionsDataTypeEnum data) {
        return 0;
    }

    @Override
    public long getEventStats(EventsDataRecordsEnum type) {
        return 0;
    }

    @Override
    public LastSynchronization getLastSynchronization() {
        return null;
    }

    @Override
    public HTTPErrors popHTTPErrors() {
        return null;
    }

    @Override
    public HTTPLatencies popHTTPLatencies(){
        return null;
    }

    @Override
    public long popAuthRejections() {
        return 0;
    }

    @Override
    public long popTokenRefreshes() {
        return 0;
    }

    @Override
    public List<StreamingEvent> popStreamingEvents() {
        return null;
    }

    @Override
    public List<String> popTags() {
        return null;
    }

    @Override
    public long getSessionLength() {
        return 0;
    }

    @Override
    public UpdatesFromSSE popUpdatesFromSSE() {
        return null;
    }
}
