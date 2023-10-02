package io.split.telemetry.storage;

import com.google.common.collect.Maps;

import io.split.telemetry.domain.HTTPErrors;
import io.split.telemetry.domain.HTTPLatencies;
import io.split.telemetry.domain.LastSynchronization;
import io.split.telemetry.domain.MethodExceptions;
import io.split.telemetry.domain.MethodLatencies;
import io.split.telemetry.domain.StreamingEvent;
import io.split.telemetry.domain.UpdatesFromSSE;
import io.split.telemetry.domain.enums.EventsDataRecordsEnum;
import io.split.telemetry.domain.enums.FactoryCountersEnum;
import io.split.telemetry.domain.enums.HTTPLatenciesEnum;
import io.split.telemetry.domain.enums.ImpressionsDataTypeEnum;
import io.split.telemetry.domain.enums.LastSynchronizationRecordsEnum;
import io.split.telemetry.domain.enums.MethodEnum;
import io.split.telemetry.domain.enums.PushCountersEnum;
import io.split.telemetry.domain.enums.ResourceEnum;
import io.split.telemetry.domain.enums.SdkRecordsEnum;
import io.split.telemetry.domain.enums.UpdatesFromSSEEnum;
import io.split.telemetry.utils.AtomicLongArray;
import io.split.telemetry.utils.BucketCalculator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryTelemetryStorage implements TelemetryStorage{
    public static final int MAX_LATENCY_BUCKET_COUNT = 23;
    public static final int MAX_STREAMING_EVENTS = 20;
    public static final int MAX_TAGS = 10;

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
    private final ConcurrentMap<UpdatesFromSSEEnum, AtomicLong> _updatesFromSSERecords = Maps.newConcurrentMap();

    //HTTPErrors
    private final ConcurrentMap<ResourceEnum, ConcurrentMap<Long, Long>> _httpErrors = Maps.newConcurrentMap();

    //StreamingEvents
    private final Object _streamingEventsLock = new Object();
    private List<StreamingEvent> _streamingEvents = new ArrayList<>();

    //Tags
    private final Object _tagsLock = new Object();
    private Set<String> _tags = new HashSet<>();

    public InMemoryTelemetryStorage() {
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
        initUpdatesFromSEE();
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
    public MethodExceptions popExceptions() {
        MethodExceptions exceptions = new MethodExceptions();
        exceptions.set_treatment(_exceptionsCounters.get(MethodEnum.TREATMENT).getAndSet(0L));
        exceptions.set_treatments(_exceptionsCounters.get(MethodEnum.TREATMENTS).getAndSet(0L));
        exceptions.set_treatmentWithConfig(_exceptionsCounters.get(MethodEnum.TREATMENT_WITH_CONFIG).getAndSet(0L));
        exceptions.set_treatmentsWithConfig(_exceptionsCounters.get(MethodEnum.TREATMENTS_WITH_CONFIG).getAndSet(0L));
        exceptions.set_treatmentByFlagSet(_exceptionsCounters.get(MethodEnum.TREATMENTS_BY_FLAG_SET).getAndSet(0L));
        exceptions.set_treatmentByFlagSets(_exceptionsCounters.get(MethodEnum.TREATMENTS_BY_FLAG_SETS).getAndSet(0L));
        exceptions.set_treatmentWithConfigByFlagSet(_exceptionsCounters.get(MethodEnum.TREATMENTS_WITH_CONFIG_BY_FLAG_SET).getAndSet(0L));
        exceptions.set_treatmentWithConfigByFlagSets(_exceptionsCounters.get(MethodEnum.TREATMENTS_WITH_CONFIG_BY_FLAG_SETS).getAndSet(0L));
        exceptions.set_track(_exceptionsCounters.get(MethodEnum.TRACK).getAndSet(0L));

        return exceptions;
    }

    @Override
    public MethodLatencies popLatencies() {
        MethodLatencies latencies = new MethodLatencies();
        latencies.set_treatment(_methodLatencies.get(MethodEnum.TREATMENT).fetchAndClearAll());
        latencies.set_treatments(_methodLatencies.get(MethodEnum.TREATMENTS).fetchAndClearAll());
        latencies.set_treatmentWithConfig(_methodLatencies.get(MethodEnum.TREATMENT_WITH_CONFIG).fetchAndClearAll());
        latencies.set_treatmentsWithConfig(_methodLatencies.get(MethodEnum.TREATMENTS_WITH_CONFIG).fetchAndClearAll());
        latencies.set_track(_methodLatencies.get(MethodEnum.TRACK).fetchAndClearAll());

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
    public HTTPLatencies popHTTPLatencies(){
        HTTPLatencies latencies = new HTTPLatencies();
        latencies.set_splits(_httpLatencies.get(HTTPLatenciesEnum.SPLITS).fetchAndClearAll());
        latencies.set_segments(_httpLatencies.get(HTTPLatenciesEnum.SEGMENTS).fetchAndClearAll());
        latencies.set_impressions(_httpLatencies.get(HTTPLatenciesEnum.IMPRESSIONS).fetchAndClearAll());
        latencies.set_impressionsCount(_httpLatencies.get(HTTPLatenciesEnum.IMPRESSIONS_COUNT).fetchAndClearAll());
        latencies.set_events(_httpLatencies.get(HTTPLatenciesEnum.EVENTS).fetchAndClearAll());
        latencies.set_telemetry(_httpLatencies.get(HTTPLatenciesEnum.TELEMETRY).fetchAndClearAll());
        latencies.set_token(_httpLatencies.get(HTTPLatenciesEnum.TOKEN).fetchAndClearAll());

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
            List<StreamingEvent> streamingEvents = _streamingEvents;
            _streamingEvents = new ArrayList<>();
            return streamingEvents;
        }
    }

    @Override
    public List<String> popTags() {
        synchronized (_tagsLock) {
            List<String> tags = new ArrayList<>(_tags);
            _tags = new HashSet<>();
            return tags;
        }
    }

    @Override
    public long getSessionLength() {
        return _sdkRecords.get(SdkRecordsEnum.SESSION).get();
    }

    @Override
    public UpdatesFromSSE popUpdatesFromSSE() {
        UpdatesFromSSE updatesFromSSE = new UpdatesFromSSE();
        updatesFromSSE.setSplits(_updatesFromSSERecords.get(UpdatesFromSSEEnum.SPLITS).getAndSet(0L));
        return updatesFromSSE;
    }

    @Override
    public void addTag(String tag) {
        synchronized (_tagsLock) {
            if(_tags.size() < MAX_TAGS) {
                _tags.add(tag);
            }
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
            if(_streamingEvents.size() < MAX_STREAMING_EVENTS) {
                _streamingEvents.add(streamingEvent);
            }
        }
    }

    @Override
    public void recordSessionLength(long sessionLength) {
        _sdkRecords.replace(SdkRecordsEnum.SESSION, new AtomicLong(sessionLength));
    }

    @Override
    public void recordUpdatesFromSSE(UpdatesFromSSEEnum updatesFromSSEEnum) {
        _updatesFromSSERecords.get(UpdatesFromSSEEnum.SPLITS).incrementAndGet();
    }

    private void initMethodLatencies() {
        _methodLatencies.put(MethodEnum.TREATMENT, new AtomicLongArray(MAX_LATENCY_BUCKET_COUNT));
        _methodLatencies.put(MethodEnum.TREATMENTS, new AtomicLongArray(MAX_LATENCY_BUCKET_COUNT));
        _methodLatencies.put(MethodEnum.TREATMENT_WITH_CONFIG, new AtomicLongArray(MAX_LATENCY_BUCKET_COUNT));
        _methodLatencies.put(MethodEnum.TREATMENTS_WITH_CONFIG, new AtomicLongArray(MAX_LATENCY_BUCKET_COUNT));
        _methodLatencies.put(MethodEnum.TRACK, new AtomicLongArray(MAX_LATENCY_BUCKET_COUNT));
    }

    private void initHttpLatencies() {
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
        _exceptionsCounters.put(MethodEnum.TREATMENTS_BY_FLAG_SET, new AtomicLong());
        _exceptionsCounters.put(MethodEnum.TREATMENTS_BY_FLAG_SETS, new AtomicLong());
        _exceptionsCounters.put(MethodEnum.TREATMENTS_WITH_CONFIG_BY_FLAG_SET, new AtomicLong());
        _exceptionsCounters.put(MethodEnum.TREATMENTS_WITH_CONFIG_BY_FLAG_SETS, new AtomicLong());
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

    private void initUpdatesFromSEE() {
        _updatesFromSSERecords.put(UpdatesFromSSEEnum.SPLITS, new AtomicLong());
    }
}