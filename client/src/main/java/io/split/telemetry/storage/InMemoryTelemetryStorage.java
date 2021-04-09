package io.split.telemetry.storage;

import com.google.common.collect.Maps;
import io.split.telemetry.domain.*;
import io.split.telemetry.domain.enums.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryTelemetryStorage implements  TelemetryStorage{

    //Latencies
    private final ConcurrentMap<MethodEnum, List<Long>> _methodLatencies = Maps.newConcurrentMap();
    private final ConcurrentMap<HTTPLatenciesEnum, List<Long>> _httpLatencies = Maps.newConcurrentMap();

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

    public InMemoryTelemetryStorage() {
        initMethodLatencies();
        initHttpLatencies();
        initHttpErrors();
    }

    @Override
    public void recordConfigData() {
        // No-Op. Config Data will be sent directly to Split Servers. No need to store.
    }

    @Override
    public long getBURTimeouts() {
        long burTimeouts = _factoryCounters.getOrDefault(FactoryCountersEnum.BUR_TIMEOUTS, new AtomicLong()).get();
        return burTimeouts;
    }

    @Override
    public long getNonReadyUsages() {
        long nonReadyUsages = _factoryCounters.getOrDefault(FactoryCountersEnum.NON_READY_USAGES, new AtomicLong()).get();
        return nonReadyUsages;
    }

    @Override
    public MethodExceptions popExceptions() {
        MethodExceptions exceptions = new MethodExceptions();
        exceptions.set_treatment(_exceptionsCounters.getOrDefault(MethodEnum.TREATMENT, new AtomicLong()).get());
        exceptions.set_treatments(_exceptionsCounters.getOrDefault(MethodEnum.TREATMENTS, new AtomicLong()).get());
        exceptions.set_treatmentWithConfig(_exceptionsCounters.getOrDefault(MethodEnum.TREATMENT_WITH_CONFIG, new AtomicLong()).get());
        exceptions.set_treatmentsWithConfig(_exceptionsCounters.getOrDefault(MethodEnum.TREATMENTS_WITH_CONFIG, new AtomicLong()).get());
        exceptions.set_track(_exceptionsCounters.getOrDefault(MethodEnum.TRACK, new AtomicLong()).get());

        _exceptionsCounters.clear();
        initMethodLatencies();

        return exceptions;
    }

    @Override
    public MethodLatencies popLatencies() {
        MethodLatencies latencies = new MethodLatencies();
        latencies.set_treatment(_methodLatencies.get(MethodEnum.TREATMENT));
        latencies.set_treatments(_methodLatencies.get(MethodEnum.TREATMENTS));
        latencies.set_treatmentWithConfig(_methodLatencies.get(MethodEnum.TREATMENT_WITH_CONFIG));
        latencies.set_treatmentsWithConfig(_methodLatencies.get(MethodEnum.TREATMENTS_WITH_CONFIG));
        latencies.set_track(_methodLatencies.get(MethodEnum.TRACK));

        _methodLatencies.clear();
        initMethodLatencies();

        return latencies;
    }

    @Override
    public void recordNonReadyUsage() {
        _factoryCounters.putIfAbsent(FactoryCountersEnum.NON_READY_USAGES, new AtomicLong(0));
        _factoryCounters.get(FactoryCountersEnum.NON_READY_USAGES).incrementAndGet();

    }

    @Override
    public void recordBURTimeout() {
        _factoryCounters.putIfAbsent(FactoryCountersEnum.BUR_TIMEOUTS, new AtomicLong(0));
        _factoryCounters.get(FactoryCountersEnum.BUR_TIMEOUTS).incrementAndGet();
    }

    @Override
    public void recordLatency(String method, int latency) {
        _methodLatencies.get(method).add(Long.valueOf(latency));
    }

    @Override
    public void recordException(MethodEnum method) {
        _exceptionsCounters.putIfAbsent(method, new AtomicLong(0));
        _exceptionsCounters.get(method).incrementAndGet();
    }

    @Override
    public long getImpressionsStats(ImpressionsDataTypeEnum data) {
        return _impressionsDataRecords.getOrDefault(data, new AtomicLong()).get();
    }

    @Override
    public long getEventStats(EventsDataRecordsEnum type) {
        return _eventsDataRecords.getOrDefault(type, new AtomicLong()).get();
    }

    @Override
    public LastSynchronization getLastSynchronization() {
        LastSynchronization lastSynchronization = new LastSynchronization();
        lastSynchronization.set_splits(_lastSynchronizationRecords.getOrDefault(LastSynchronizationRecordsEnum.SPLITS, new AtomicLong()).get());
        lastSynchronization.set_segments(_lastSynchronizationRecords.getOrDefault(LastSynchronizationRecordsEnum.SEGMENTS, new AtomicLong()).get());
        lastSynchronization.set_impressions(_lastSynchronizationRecords.getOrDefault(LastSynchronizationRecordsEnum.IMPRESSIONS, new AtomicLong()).get());
        lastSynchronization.set_events(_lastSynchronizationRecords.getOrDefault(LastSynchronizationRecordsEnum.EVENTS, new AtomicLong()).get());
        lastSynchronization.set_telemetry(_lastSynchronizationRecords.getOrDefault(LastSynchronizationRecordsEnum.TELEMETRY, new AtomicLong()).get());
        lastSynchronization.set_token(_lastSynchronizationRecords.getOrDefault(LastSynchronizationRecordsEnum.TOKEN, new AtomicLong()).get());

        return lastSynchronization;
    }

    @Override
    public HTTPErrors popHTTPErrors() {
        HTTPErrors errors = new HTTPErrors();
        errors.set_splits(_httpErrors.get(ResourceEnum.SPLIT_SYNC));
        errors.set_segments(_httpErrors.get(ResourceEnum.SEGMENT_SYNC));
        errors.set_impressions(_httpErrors.get(ResourceEnum.IMPRESSION_SYNC));
        errors.set_events(_httpErrors.get(ResourceEnum.EVENT_SYNC));
        errors.set_telemetry(_httpErrors.get(ResourceEnum.TELEMETRY_SYNC));
        errors.set_token(_httpErrors.get(ResourceEnum.TOKEN_SYNC));

        _httpErrors.clear();
        initHttpErrors();

        return errors;
    }

    @Override
    public HTTPLatencies popHTTPLatencies() {
        HTTPLatencies latencies = new HTTPLatencies();
        latencies.set_splits(_httpLatencies.get(HTTPLatenciesEnum.SPLITS));
        latencies.set_segments(_httpLatencies.get(HTTPLatenciesEnum.SEGMENTS));
        latencies.set_impressions(_httpLatencies.get(HTTPLatenciesEnum.IMPRESSIONS));
        latencies.set_events(_httpLatencies.get(HTTPLatenciesEnum.EVENTS));
        latencies.set_telemetry(_httpLatencies.get(HTTPLatenciesEnum.TELEMETRY));
        latencies.set_token(_httpLatencies.get(HTTPLatenciesEnum.TOKEN));

        _httpLatencies.clear();
        initHttpLatencies();

        return latencies;
    }

    @Override
    public long popAuthRejections() {
        long authRejections = _pushCounters.getOrDefault(PushCountersEnum.AUTH_REJECTIONS, new AtomicLong()).get();

        _pushCounters.replace(PushCountersEnum.AUTH_REJECTIONS, new AtomicLong());

        return authRejections;
    }

    @Override
    public long popTokenRefreshes() {
        long tokenRefreshes = _pushCounters.getOrDefault(PushCountersEnum.TOKEN_REFRESHES, new AtomicLong()).get();

        _pushCounters.replace(PushCountersEnum.TOKEN_REFRESHES, new AtomicLong());

        return tokenRefreshes;
    }

    @Override
    public List<StreamingEvent> popStreamingEvents() {
        synchronized (_streamingEventsLock) {
            List<StreamingEvent> streamingEvents = _streamingEvents;

            _streamingEvents.clear();

            return streamingEvents;
        }
    }

    @Override
    public List<String> popTags() {
        synchronized (_tagsLock) {
            List<String> tags = _tags;

            _tags.clear();

            return tags;
        }
    }

    @Override
    public long getSessionLength() {
        return _sdkRecords.getOrDefault(SdkRecordsEnum.SESSION, new AtomicLong()).get();
    }

    @Override
    public void addTag(String tag) {
        synchronized (_tagsLock) {
            _tags.add(tag);
        }
    }

    @Override
    public void recordImpressionStats(ImpressionsDataTypeEnum dataType, long count) {
        _impressionsDataRecords.putIfAbsent(dataType, new AtomicLong());
        _impressionsDataRecords.get(dataType).incrementAndGet();
    }

    @Override
    public void recordEventStats(EventsDataRecordsEnum dataType, long count) {
        _eventsDataRecords.putIfAbsent(dataType, new AtomicLong());
        _eventsDataRecords.get(dataType).incrementAndGet();
    }

    @Override
    public void recordSuccessfulSync(LastSynchronizationRecordsEnum resource, long time) {
        _lastSynchronizationRecords.putIfAbsent(resource, new AtomicLong(time));

    }

    @Override
    public void recordSyncError(ResourceEnum resource, int status) {
        ConcurrentMap<Long, Long> errors = _httpErrors.get(resource);
        errors.putIfAbsent(Long.valueOf(status), 0l);
        errors.replace(Long.valueOf(status), errors.get(status) + 1);
    }

    @Override
    public void recordSyncLatency(String resource, long latency) {
        _httpLatencies.get(resource).add(latency);

    }

    @Override
    public void recordAuthRejections() {
        _pushCounters.putIfAbsent(PushCountersEnum.AUTH_REJECTIONS, new AtomicLong(0));
        _pushCounters.get(PushCountersEnum.AUTH_REJECTIONS).incrementAndGet();

    }

    @Override
    public void recordTokenRefreshes() {
        _pushCounters.putIfAbsent(PushCountersEnum.TOKEN_REFRESHES, new AtomicLong(0));
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
        _sdkRecords.putIfAbsent(SdkRecordsEnum.SESSION, new AtomicLong(sessionLength));
    }

    private void initMethodLatencies() {
        _methodLatencies.put(MethodEnum.TREATMENT, new ArrayList<>());
        _methodLatencies.put(MethodEnum.TREATMENTS, new ArrayList<>());
        _methodLatencies.put(MethodEnum.TREATMENT_WITH_CONFIG, new ArrayList<>());
        _methodLatencies.put(MethodEnum.TREATMENTS_WITH_CONFIG, new ArrayList<>());
        _methodLatencies.put(MethodEnum.TRACK, new ArrayList<>());
    }

    private void initHttpLatencies() {
        _httpLatencies.put(HTTPLatenciesEnum.SPLITS, new ArrayList<>());
        _httpLatencies.put(HTTPLatenciesEnum.SEGMENTS, new ArrayList<>());
        _httpLatencies.put(HTTPLatenciesEnum.IMPRESSIONS, new ArrayList<>());
        _httpLatencies.put(HTTPLatenciesEnum.EVENTS, new ArrayList<>());
        _httpLatencies.put(HTTPLatenciesEnum.TELEMETRY, new ArrayList<>());
        _httpLatencies.put(HTTPLatenciesEnum.TOKEN, new ArrayList<>());
    }

    private void initHttpErrors() {
        _httpErrors.put(ResourceEnum.SPLIT_SYNC, Maps.newConcurrentMap());
        _httpErrors.put(ResourceEnum.SEGMENT_SYNC, Maps.newConcurrentMap());
        _httpErrors.put(ResourceEnum.IMPRESSION_SYNC, Maps.newConcurrentMap());
        _httpErrors.put(ResourceEnum.EVENT_SYNC, Maps.newConcurrentMap());
        _httpErrors.put(ResourceEnum.TELEMETRY_SYNC, Maps.newConcurrentMap());
        _httpErrors.put(ResourceEnum.TOKEN_SYNC, Maps.newConcurrentMap());
    }
}
