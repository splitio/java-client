package io.split.telemetry.storage;

import com.google.common.collect.Maps;
import io.split.telemetry.utils.BucketCalculator;
import io.split.telemetry.domain.*;
import io.split.telemetry.domain.enums.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class InMemoryTelemetryStorage implements  TelemetryStorage{
    public static final int MAX_LATENCY_BUCKET_COUNT = 23;

    //Latencies
    private final ConcurrentMap<MethodEnum, AtomicLongArray> _methodLatencies = Maps.newConcurrentMap();
    private final ConcurrentMap<HTTPLatenciesEnum, AtomicLongArray> _httpLatencies = Maps.newConcurrentMap();

    //Counters
    private final ConcurrentMap<MethodEnum, AtomicLong> _exceptionsCounters = Maps.newConcurrentMap();
    private final ConcurrentMap<PushCountersEnum, AtomicLong> _pushCounters = Maps.newConcurrentMap();
    private final ConcurrentMap<FactoryCountersEnum, AtomicLong> _factoryCounters = Maps.newConcurrentMap();

    //Records
    private final ConcurrentMap<ImpressionsDataTypeEnum, AtomicLong> _impressionsDataRecords = Maps.newConcurrentMap();
    private final ConcurrentMap<EventsDataRecordsEnum, AtomicLong> _eventsDataRecords = Maps.newConcurrentMap();
    private final ConcurrentMap<LastSynchronizationRecordsEnum, AtomicLong> _lastSynchronizationRecords = Maps.newConcurrentMap();
    private final ConcurrentMap<SdkRecordsEnum, AtomicLong> _sdkRecords = Maps.newConcurrentMap();

    //HTTPErrors
    private final ConcurrentMap<ResourceEnum, ConcurrentMap<Long, Long>> _httpErrors = Maps.newConcurrentMap();

    //StreamingEvents
    private final Object _streamingEventsLock = new Object();
    private final List<StreamingEvent> _streamingEvents = new ArrayList<>();

    //Tags
    private final Object _tagsLock = new Object();
    private final List<String> _tags = new ArrayList<>();

    public InMemoryTelemetryStorage() throws Exception {
        initMethodLatencies();
        initHttpLatencies();
        initHttpErrors();
        initMethodExceptions();
        initFactoryCounters();
        initImpressionDataCounters();
        initPushCounters();
        initSdkRecords();
        initLastSynchronizationRecords();
        initEventDataRecords();
    }

    @Override
    public long getBURTimeouts() {
        return _factoryCounters.get(FactoryCountersEnum.BUR_TIMEOUTS).get();
    }

    @Override
    public long getNonReadyUsages() {
        return _factoryCounters.get(FactoryCountersEnum.NON_READY_USAGES).get();
    }

    @Override
    public MethodExceptions popExceptions() throws Exception {
        MethodExceptions exceptions = new MethodExceptions();
        exceptions.set_treatment(_exceptionsCounters.get(MethodEnum.TREATMENT).get());
        exceptions.set_treatments(_exceptionsCounters.get(MethodEnum.TREATMENTS).get());
        exceptions.set_treatmentWithConfig(_exceptionsCounters.get(MethodEnum.TREATMENT_WITH_CONFIG).get());
        exceptions.set_treatmentsWithConfig(_exceptionsCounters.get(MethodEnum.TREATMENTS_WITH_CONFIG).get());
        exceptions.set_track(_exceptionsCounters.get(MethodEnum.TRACK).get());

        _exceptionsCounters.clear();
        initMethodExceptions();

        return exceptions;
    }

    @Override
    public MethodLatencies popLatencies() throws Exception {
        MethodLatencies latencies = new MethodLatencies();
        latencies.set_treatment(_methodLatencies.get(MethodEnum.TREATMENT).fetchAndClearAll());
        latencies.set_treatments(_methodLatencies.get(MethodEnum.TREATMENTS).fetchAndClearAll());
        latencies.set_treatmentWithConfig(_methodLatencies.get(MethodEnum.TREATMENT_WITH_CONFIG).fetchAndClearAll());
        latencies.set_treatmentsWithConfig(_methodLatencies.get(MethodEnum.TREATMENTS_WITH_CONFIG).fetchAndClearAll());
        latencies.set_track(_methodLatencies.get(MethodEnum.TRACK).fetchAndClearAll());

        _methodLatencies.clear();
        initMethodLatencies();

        return latencies;
    }

    @Override
    public void recordNonReadyUsage() {
        _factoryCounters.get(FactoryCountersEnum.NON_READY_USAGES).incrementAndGet();
    }

    @Override
    public void recordBURTimeout() {
        _factoryCounters.get(FactoryCountersEnum.BUR_TIMEOUTS).incrementAndGet();
    }


    @Override
    public void recordLatency(MethodEnum method, long latency) {
        int bucket = BucketCalculator.getBucketForLatency(latency);
        _methodLatencies.get(method).increment(bucket);
    }

    @Override
    public void recordException(MethodEnum method) {
        _exceptionsCounters.get(method).incrementAndGet();
    }

    @Override
    public long getImpressionsStats(ImpressionsDataTypeEnum dataType) {
        return _impressionsDataRecords.get(dataType).get();
    }

    @Override
    public long getEventStats(EventsDataRecordsEnum dataType) {
        return _eventsDataRecords.get(dataType).get();
    }

    @Override
    public LastSynchronization getLastSynchronization() {
        LastSynchronization lastSynchronization = new LastSynchronization();
        lastSynchronization.set_splits(_lastSynchronizationRecords.get(LastSynchronizationRecordsEnum.SPLITS).get());
        lastSynchronization.set_segments(_lastSynchronizationRecords.get(LastSynchronizationRecordsEnum.SEGMENTS).get());
        lastSynchronization.set_impressions(_lastSynchronizationRecords.get(LastSynchronizationRecordsEnum.IMPRESSIONS).get());
        lastSynchronization.set_impressionsCount(_lastSynchronizationRecords.get(LastSynchronizationRecordsEnum.IMPRESSIONS_COUNT).get());
        lastSynchronization.set_events(_lastSynchronizationRecords.get(LastSynchronizationRecordsEnum.EVENTS).get());
        lastSynchronization.set_telemetry(_lastSynchronizationRecords.get(LastSynchronizationRecordsEnum.TELEMETRY).get());
        lastSynchronization.set_token(_lastSynchronizationRecords.get(LastSynchronizationRecordsEnum.TOKEN).get());

        return lastSynchronization;
    }

    @Override
    public HTTPErrors popHTTPErrors() {
        HTTPErrors errors = new HTTPErrors();
        errors.set_splits(_httpErrors.get(ResourceEnum.SPLIT_SYNC));
        errors.set_segments(_httpErrors.get(ResourceEnum.SEGMENT_SYNC));
        errors.set_impressions(_httpErrors.get(ResourceEnum.IMPRESSION_SYNC));
        errors.set_impressionsCount(_httpErrors.get(ResourceEnum.IMPRESSION_COUNT_SYNC));
        errors.set_events(_httpErrors.get(ResourceEnum.EVENT_SYNC));
        errors.set_telemetry(_httpErrors.get(ResourceEnum.TELEMETRY_SYNC));
        errors.set_token(_httpErrors.get(ResourceEnum.TOKEN_SYNC));

        _httpErrors.clear();
        initHttpErrors();

        return errors;
    }

    @Override
    public HTTPLatencies popHTTPLatencies() throws Exception {
        HTTPLatencies latencies = new HTTPLatencies();
        latencies.set_splits(_httpLatencies.get(HTTPLatenciesEnum.SPLITS).fetchAndClearAll());
        latencies.set_segments(_httpLatencies.get(HTTPLatenciesEnum.SEGMENTS).fetchAndClearAll());
        latencies.set_impressions(_httpLatencies.get(HTTPLatenciesEnum.IMPRESSIONS).fetchAndClearAll());
        latencies.set_impressionsCount(_httpLatencies.get(HTTPLatenciesEnum.IMPRESSIONS_COUNT).fetchAndClearAll());
        latencies.set_events(_httpLatencies.get(HTTPLatenciesEnum.EVENTS).fetchAndClearAll());
        latencies.set_telemetry(_httpLatencies.get(HTTPLatenciesEnum.TELEMETRY).fetchAndClearAll());
        latencies.set_token(_httpLatencies.get(HTTPLatenciesEnum.TOKEN).fetchAndClearAll());

        _httpLatencies.clear();
        initHttpLatencies();

        return latencies;
    }

    @Override
    public long popAuthRejections() {
        long authRejections = _pushCounters.get(PushCountersEnum.AUTH_REJECTIONS).get();

        _pushCounters.replace(PushCountersEnum.AUTH_REJECTIONS, new AtomicLong());

        return authRejections;
    }

    @Override
    public long popTokenRefreshes() {
        long tokenRefreshes = _pushCounters.get(PushCountersEnum.TOKEN_REFRESHES).get();

        _pushCounters.replace(PushCountersEnum.TOKEN_REFRESHES, new AtomicLong());

        return tokenRefreshes;
    }

    @Override
    public List<StreamingEvent> popStreamingEvents() {
        synchronized (_streamingEventsLock) {
            List<StreamingEvent> streamingEvents = _streamingEvents.stream().collect(Collectors.toList());

            _streamingEvents.clear();

            return streamingEvents;
        }
    }

    @Override
    public List<String> popTags() {
        synchronized (_tagsLock) {
            List<String> tags = _tags.stream().collect(Collectors.toList());

            _tags.clear();

            return tags;
        }
    }

    @Override
    public long getSessionLength() {
        return _sdkRecords.get(SdkRecordsEnum.SESSION).get();
    }

    @Override
    public void addTag(String tag) {
        synchronized (_tagsLock) {
            _tags.add(tag);
        }
    }

    @Override
    public void recordImpressionStats(ImpressionsDataTypeEnum dataType, long count) {
        _impressionsDataRecords.get(dataType).addAndGet(count);
    }

    @Override
    public void recordEventStats(EventsDataRecordsEnum dataType, long count) {
        _eventsDataRecords.get(dataType).addAndGet(count);
    }

    @Override
    public void recordSuccessfulSync(LastSynchronizationRecordsEnum resource, long time) {
        _lastSynchronizationRecords.replace(resource, new AtomicLong(time));
    }

    @Override
    public void recordSyncError(ResourceEnum resource, int status) {
        ConcurrentMap<Long, Long> errors = _httpErrors.get(resource);
        errors.putIfAbsent(Long.valueOf(status), 0l);
        errors.replace(Long.valueOf(status), errors.get(Long.valueOf(status)) + 1);
    }

    @Override
    public void recordSyncLatency(HTTPLatenciesEnum resource, long latency) {
        int bucket = BucketCalculator.getBucketForLatency(latency);
        _httpLatencies.get(resource).increment(bucket);

    }

    @Override
    public void recordAuthRejections() {
        _pushCounters.get(PushCountersEnum.AUTH_REJECTIONS).incrementAndGet();
    }

    @Override
    public void recordTokenRefreshes() {
        _pushCounters.get(PushCountersEnum.TOKEN_REFRESHES).incrementAndGet();
    }

    @Override
    public void recordStreamingEvents(StreamingEvent streamingEvent) {
        synchronized (_streamingEventsLock) {
            _streamingEvents.add(streamingEvent);
        }
    }

    @Override
    public void recordSessionLength(long sessionLength) {
        _sdkRecords.replace(SdkRecordsEnum.SESSION, new AtomicLong(sessionLength));
    }

    private void initMethodLatencies() throws Exception {
        _methodLatencies.put(MethodEnum.TREATMENT, new AtomicLongArray(MAX_LATENCY_BUCKET_COUNT));
        _methodLatencies.put(MethodEnum.TREATMENTS, new AtomicLongArray(MAX_LATENCY_BUCKET_COUNT));
        _methodLatencies.put(MethodEnum.TREATMENT_WITH_CONFIG, new AtomicLongArray(MAX_LATENCY_BUCKET_COUNT));
        _methodLatencies.put(MethodEnum.TREATMENTS_WITH_CONFIG, new AtomicLongArray(MAX_LATENCY_BUCKET_COUNT));
        _methodLatencies.put(MethodEnum.TRACK, new AtomicLongArray(MAX_LATENCY_BUCKET_COUNT));
    }

    private void initHttpLatencies() throws Exception {
        _httpLatencies.put(HTTPLatenciesEnum.SPLITS, new AtomicLongArray(MAX_LATENCY_BUCKET_COUNT));
        _httpLatencies.put(HTTPLatenciesEnum.SEGMENTS, new AtomicLongArray(MAX_LATENCY_BUCKET_COUNT));
        _httpLatencies.put(HTTPLatenciesEnum.IMPRESSIONS, new AtomicLongArray(MAX_LATENCY_BUCKET_COUNT));
        _httpLatencies.put(HTTPLatenciesEnum.IMPRESSIONS_COUNT, new AtomicLongArray(MAX_LATENCY_BUCKET_COUNT));
        _httpLatencies.put(HTTPLatenciesEnum.EVENTS, new AtomicLongArray(MAX_LATENCY_BUCKET_COUNT));
        _httpLatencies.put(HTTPLatenciesEnum.TELEMETRY, new AtomicLongArray(MAX_LATENCY_BUCKET_COUNT));
        _httpLatencies.put(HTTPLatenciesEnum.TOKEN, new AtomicLongArray(MAX_LATENCY_BUCKET_COUNT));
    }

    private void initHttpErrors() {
        _httpErrors.put(ResourceEnum.SPLIT_SYNC, Maps.newConcurrentMap());
        _httpErrors.put(ResourceEnum.SEGMENT_SYNC, Maps.newConcurrentMap());
        _httpErrors.put(ResourceEnum.IMPRESSION_SYNC, Maps.newConcurrentMap());
        _httpErrors.put(ResourceEnum.IMPRESSION_COUNT_SYNC, Maps.newConcurrentMap());
        _httpErrors.put(ResourceEnum.EVENT_SYNC, Maps.newConcurrentMap());
        _httpErrors.put(ResourceEnum.TELEMETRY_SYNC, Maps.newConcurrentMap());
        _httpErrors.put(ResourceEnum.TOKEN_SYNC, Maps.newConcurrentMap());
    }

    private void initMethodExceptions() {
        _exceptionsCounters.put(MethodEnum.TREATMENT, new AtomicLong());
        _exceptionsCounters.put(MethodEnum.TREATMENTS, new AtomicLong());
        _exceptionsCounters.put(MethodEnum.TREATMENT_WITH_CONFIG, new AtomicLong());
        _exceptionsCounters.put(MethodEnum.TREATMENTS_WITH_CONFIG, new AtomicLong());
        _exceptionsCounters.put(MethodEnum.TRACK, new AtomicLong());
    }

    private void initFactoryCounters() {
        _factoryCounters.put(FactoryCountersEnum.BUR_TIMEOUTS, new AtomicLong());
        _factoryCounters.put(FactoryCountersEnum.NON_READY_USAGES, new AtomicLong());
    }

    private void initImpressionDataCounters() {
        _impressionsDataRecords.put(ImpressionsDataTypeEnum.IMPRESSIONS_DEDUPED, new AtomicLong());
        _impressionsDataRecords.put(ImpressionsDataTypeEnum.IMPRESSIONS_DROPPED, new AtomicLong());
        _impressionsDataRecords.put(ImpressionsDataTypeEnum.IMPRESSIONS_QUEUED, new AtomicLong());
    }

    private void initPushCounters() {
        _pushCounters.put(PushCountersEnum.AUTH_REJECTIONS, new AtomicLong());
        _pushCounters.put(PushCountersEnum.TOKEN_REFRESHES, new AtomicLong());
    }

    private void initSdkRecords() {
        _sdkRecords.put(SdkRecordsEnum.SESSION, new AtomicLong());
    }

    private void initLastSynchronizationRecords() {
        _lastSynchronizationRecords.put(LastSynchronizationRecordsEnum.SPLITS, new AtomicLong());
        _lastSynchronizationRecords.put(LastSynchronizationRecordsEnum.SEGMENTS, new AtomicLong());
        _lastSynchronizationRecords.put(LastSynchronizationRecordsEnum.EVENTS, new AtomicLong());
        _lastSynchronizationRecords.put(LastSynchronizationRecordsEnum.IMPRESSIONS, new AtomicLong());
        _lastSynchronizationRecords.put(LastSynchronizationRecordsEnum.IMPRESSIONS_COUNT, new AtomicLong());
        _lastSynchronizationRecords.put(LastSynchronizationRecordsEnum.TOKEN, new AtomicLong());
        _lastSynchronizationRecords.put(LastSynchronizationRecordsEnum.TELEMETRY, new AtomicLong());
    }

    private void initEventDataRecords() {
        _eventsDataRecords.put(EventsDataRecordsEnum.EVENTS_DROPPED, new AtomicLong());
        _eventsDataRecords.put(EventsDataRecordsEnum.EVENTS_QUEUED, new AtomicLong());
    }
}
