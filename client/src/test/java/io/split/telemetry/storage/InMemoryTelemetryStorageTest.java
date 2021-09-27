package io.split.telemetry.storage;

import io.split.telemetry.domain.*;
import io.split.telemetry.domain.enums.*;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class InMemoryTelemetryStorageTest{

    @Test
    public void testInMemoryTelemetryStorage() throws Exception {
        InMemoryTelemetryStorage telemetryStorage = new InMemoryTelemetryStorage();

        //MethodLatencies
        telemetryStorage.recordLatency(MethodEnum.TREATMENT, 1500l * 1000);
        telemetryStorage.recordLatency(MethodEnum.TREATMENT, 2000l * 1000);
        telemetryStorage.recordLatency(MethodEnum.TREATMENTS, 3000l * 1000);
        telemetryStorage.recordLatency(MethodEnum.TREATMENTS, 500l * 1000);
        telemetryStorage.recordLatency(MethodEnum.TREATMENT_WITH_CONFIG, 800l * 1000);
        telemetryStorage.recordLatency(MethodEnum.TREATMENTS_WITH_CONFIG, 1000l * 1000);

        MethodLatencies latencies = telemetryStorage.popLatencies();
        Assert.assertEquals(2, latencies.get_treatment().stream().mapToInt(Long::intValue).sum());
        Assert.assertEquals(2, latencies.get_treatments().stream().mapToInt(Long::intValue).sum());
        Assert.assertEquals(1, latencies.get_treatmentsWithConfig().stream().mapToInt(Long::intValue).sum());
        Assert.assertEquals(1, latencies.get_treatmentWithConfig().stream().mapToInt(Long::intValue).sum());
        Assert.assertEquals(0, latencies.get_track().stream().mapToInt(Long::intValue).sum());

        //Check empty has worked
        latencies = telemetryStorage.popLatencies();
        Assert.assertEquals(0, latencies.get_treatment().stream().mapToInt(Long::intValue).sum());
        Assert.assertEquals(0, latencies.get_treatments().stream().mapToInt(Long::intValue).sum());
        Assert.assertEquals(0, latencies.get_treatmentsWithConfig().stream().mapToInt(Long::intValue).sum());
        Assert.assertEquals(0, latencies.get_treatmentWithConfig().stream().mapToInt(Long::intValue).sum());
        Assert.assertEquals(0, latencies.get_track().stream().mapToInt(Long::intValue).sum());

        //HttpLatencies
        telemetryStorage.recordSyncLatency(HTTPLatenciesEnum.TELEMETRY, 1500l * 1000);
        telemetryStorage.recordSyncLatency(HTTPLatenciesEnum.TELEMETRY, 2000l * 1000);
        telemetryStorage.recordSyncLatency(HTTPLatenciesEnum.EVENTS, 1500l * 1000);
        telemetryStorage.recordSyncLatency(HTTPLatenciesEnum.EVENTS, 2000l * 1000);
        telemetryStorage.recordSyncLatency(HTTPLatenciesEnum.SEGMENTS, 1500l * 1000);
        telemetryStorage.recordSyncLatency(HTTPLatenciesEnum.SPLITS, 2000l * 1000);
        telemetryStorage.recordSyncLatency(HTTPLatenciesEnum.SPLITS, 1500l * 1000);
        telemetryStorage.recordSyncLatency(HTTPLatenciesEnum.SPLITS, 2000l * 1000);
        telemetryStorage.recordSyncLatency(HTTPLatenciesEnum.IMPRESSIONS, 1500l * 1000);
        telemetryStorage.recordSyncLatency(HTTPLatenciesEnum.IMPRESSIONS_COUNT, 2000l * 1000);

        HTTPLatencies httpLatencies = telemetryStorage.popHTTPLatencies();

        Assert.assertEquals(3, httpLatencies.get_splits().stream().mapToInt(Long::intValue).sum());
        Assert.assertEquals(2, httpLatencies.get_telemetry().stream().mapToInt(Long::intValue).sum());
        Assert.assertEquals(2, httpLatencies.get_events().stream().mapToInt(Long::intValue).sum());
        Assert.assertEquals(1, httpLatencies.get_segments().stream().mapToInt(Long::intValue).sum());
        Assert.assertEquals(1, httpLatencies.get_impressions().stream().mapToInt(Long::intValue).sum());
        Assert.assertEquals(1, httpLatencies.get_impressionsCount().stream().mapToInt(Long::intValue).sum());
        Assert.assertEquals(0, httpLatencies.get_token().stream().mapToInt(Long::intValue).sum());

        httpLatencies = telemetryStorage.popHTTPLatencies();
        Assert.assertEquals(0, httpLatencies.get_splits().stream().mapToInt(Long::intValue).sum());
        Assert.assertEquals(0, httpLatencies.get_telemetry().stream().mapToInt(Long::intValue).sum());
        Assert.assertEquals(0, httpLatencies.get_events().stream().mapToInt(Long::intValue).sum());
        Assert.assertEquals(0, httpLatencies.get_segments().stream().mapToInt(Long::intValue).sum());
        Assert.assertEquals(0, httpLatencies.get_impressions().stream().mapToInt(Long::intValue).sum());
        Assert.assertEquals(0, httpLatencies.get_impressionsCount().stream().mapToInt(Long::intValue).sum());
        Assert.assertEquals(0, httpLatencies.get_token().stream().mapToInt(Long::intValue).sum());


        //Exceptions
        telemetryStorage.recordException(MethodEnum.TREATMENT);
        telemetryStorage.recordException(MethodEnum.TREATMENTS);
        telemetryStorage.recordException(MethodEnum.TREATMENT);
        telemetryStorage.recordException(MethodEnum.TREATMENTS);
        telemetryStorage.recordException(MethodEnum.TREATMENT_WITH_CONFIG);
        telemetryStorage.recordException(MethodEnum.TREATMENTS_WITH_CONFIG);

        MethodExceptions methodExceptions = telemetryStorage.popExceptions();
        Assert.assertEquals(2, methodExceptions.get_treatment());
        Assert.assertEquals(2, methodExceptions.get_treatments());
        Assert.assertEquals(1, methodExceptions.get_treatmentsWithConfig());
        Assert.assertEquals(1, methodExceptions.get_treatmentWithConfig());
        Assert.assertEquals(0, methodExceptions.get_track());

        //Check empty has worked
        methodExceptions = telemetryStorage.popExceptions();
        Assert.assertEquals(0, methodExceptions.get_treatment());
        Assert.assertEquals(0, methodExceptions.get_treatments());
        Assert.assertEquals(0, methodExceptions.get_treatmentsWithConfig());
        Assert.assertEquals(0, methodExceptions.get_treatmentWithConfig());
        Assert.assertEquals(0, methodExceptions.get_track());

        //AuthRejections
        telemetryStorage.recordAuthRejections();
        long authRejections = telemetryStorage.popAuthRejections();
        Assert.assertEquals(1, authRejections);

        //Check amount has been reseted
        authRejections = telemetryStorage.popAuthRejections();
        Assert.assertEquals(0, authRejections);

        //AuthRejections
        telemetryStorage.recordTokenRefreshes();
        telemetryStorage.recordTokenRefreshes();
        long tokenRefreshes = telemetryStorage.popTokenRefreshes();
        Assert.assertEquals(2, tokenRefreshes);

        //Check amount has been reseted
        tokenRefreshes = telemetryStorage.popTokenRefreshes();
        Assert.assertEquals(0, tokenRefreshes);

        //Non Ready usages
        telemetryStorage.recordNonReadyUsage();
        telemetryStorage.recordNonReadyUsage();
        long nonReadyUsages = telemetryStorage.getNonReadyUsages();
        Assert.assertEquals(2, nonReadyUsages);

        //BUR Timeouts
        telemetryStorage.recordBURTimeout();
        long burTimeouts = telemetryStorage.getBURTimeouts();
        Assert.assertEquals(1, burTimeouts);

        //ImpressionStats
        telemetryStorage.recordImpressionStats(ImpressionsDataTypeEnum.IMPRESSIONS_DEDUPED, 3);
        telemetryStorage.recordImpressionStats(ImpressionsDataTypeEnum.IMPRESSIONS_DEDUPED, 1);
        telemetryStorage.recordImpressionStats(ImpressionsDataTypeEnum.IMPRESSIONS_DROPPED, 4);
        telemetryStorage.recordImpressionStats(ImpressionsDataTypeEnum.IMPRESSIONS_DROPPED, 6);
        telemetryStorage.recordImpressionStats(ImpressionsDataTypeEnum.IMPRESSIONS_DROPPED, 2);

        long impressionsDeduped = telemetryStorage.getImpressionsStats(ImpressionsDataTypeEnum.IMPRESSIONS_DEDUPED);
        long impressionsDropped = telemetryStorage.getImpressionsStats(ImpressionsDataTypeEnum.IMPRESSIONS_DROPPED);
        long impressionsQueued = telemetryStorage.getImpressionsStats(ImpressionsDataTypeEnum.IMPRESSIONS_QUEUED);

        Assert.assertEquals(4, impressionsDeduped);
        Assert.assertEquals(12, impressionsDropped);
        Assert.assertEquals(0, impressionsQueued);

        //Event Stats
        telemetryStorage.recordEventStats(EventsDataRecordsEnum.EVENTS_DROPPED, 3);
        telemetryStorage.recordEventStats(EventsDataRecordsEnum.EVENTS_DROPPED, 7);
        telemetryStorage.recordEventStats(EventsDataRecordsEnum.EVENTS_QUEUED, 3);

        long eventsDropped = telemetryStorage.getEventStats(EventsDataRecordsEnum.EVENTS_DROPPED);
        long eventsQueued = telemetryStorage.getEventStats(EventsDataRecordsEnum.EVENTS_QUEUED);

        Assert.assertEquals(10, eventsDropped);
        Assert.assertEquals(3, eventsQueued);

        //Successfuly sync
        telemetryStorage.recordSuccessfulSync(LastSynchronizationRecordsEnum.EVENTS, 1500);
        telemetryStorage.recordSuccessfulSync(LastSynchronizationRecordsEnum.EVENTS, 800);
        telemetryStorage.recordSuccessfulSync(LastSynchronizationRecordsEnum.IMPRESSIONS, 2500);
        telemetryStorage.recordSuccessfulSync(LastSynchronizationRecordsEnum.IMPRESSIONS, 10500);
        telemetryStorage.recordSuccessfulSync(LastSynchronizationRecordsEnum.IMPRESSIONS_COUNT, 1500);
        telemetryStorage.recordSuccessfulSync(LastSynchronizationRecordsEnum.SEGMENTS, 1580);
        telemetryStorage.recordSuccessfulSync(LastSynchronizationRecordsEnum.TELEMETRY, 265);
        telemetryStorage.recordSuccessfulSync(LastSynchronizationRecordsEnum.TOKEN, 129);

        LastSynchronization lastSynchronization = telemetryStorage.getLastSynchronization();
        Assert.assertEquals(800, lastSynchronization.get_events());
        Assert.assertEquals(129, lastSynchronization.get_token());
        Assert.assertEquals(1580, lastSynchronization.get_segments());
        Assert.assertEquals(0, lastSynchronization.get_splits());
        Assert.assertEquals(10500, lastSynchronization.get_impressions());
        Assert.assertEquals(1500, lastSynchronization.get_impressionsCount());
        Assert.assertEquals(265, lastSynchronization.get_telemetry());

        //Session length
        telemetryStorage.recordSessionLength(91218);
        long sessionLength = telemetryStorage.getSessionLength();
        Assert.assertEquals(91218, sessionLength);

        //Sync Error
        telemetryStorage.recordSyncError(ResourceEnum.TELEMETRY_SYNC, 400);
        telemetryStorage.recordSyncError(ResourceEnum.TELEMETRY_SYNC, 400);
        telemetryStorage.recordSyncError(ResourceEnum.SEGMENT_SYNC, 501);
        telemetryStorage.recordSyncError(ResourceEnum.IMPRESSION_SYNC, 403);
        telemetryStorage.recordSyncError(ResourceEnum.IMPRESSION_SYNC, 403);
        telemetryStorage.recordSyncError(ResourceEnum.EVENT_SYNC, 503);
        telemetryStorage.recordSyncError(ResourceEnum.SPLIT_SYNC, 403);
        telemetryStorage.recordSyncError(ResourceEnum.IMPRESSION_COUNT_SYNC, 403);
        telemetryStorage.recordSyncError(ResourceEnum.TOKEN_SYNC, 403);

        HTTPErrors httpErrors = telemetryStorage.popHTTPErrors();
        Assert.assertEquals(2, httpErrors.get_telemetry().get(400l).intValue());
        Assert.assertEquals(1, httpErrors.get_segments().get(501l).intValue());
        Assert.assertEquals(2, httpErrors.get_impressions().get(403l).intValue());
        Assert.assertEquals(1, httpErrors.get_impressionsCount().get(403l).intValue());
        Assert.assertEquals(1, httpErrors.get_events().get(503l).intValue());
        Assert.assertEquals(1, httpErrors.get_splits().get(403l).intValue());
        Assert.assertEquals(1, httpErrors.get_token().get(403l).intValue());

        //Streaming events
        StreamingEvent streamingEvent = new StreamingEvent(1, 290, 91218);
        telemetryStorage.recordStreamingEvents(streamingEvent);

        List<StreamingEvent> streamingEvents = telemetryStorage.popStreamingEvents();
        Assert.assertEquals(290, streamingEvents.get(0).get_data());
        Assert.assertEquals(1, streamingEvents.get(0).get_type());
        Assert.assertEquals(91218, streamingEvents.get(0).getTimestamp());

        //Check list has been cleared
        streamingEvents = telemetryStorage.popStreamingEvents();
        Assert.assertEquals(0, streamingEvents.size());

        //Tags
        telemetryStorage.addTag("TAG_1");
        telemetryStorage.addTag("TAG_2");
        telemetryStorage.addTag("TAG_2");
        List<String> tags = telemetryStorage.popTags();
        Assert.assertEquals(2, tags.size());

        //Check tags have been cleared
        tags = telemetryStorage.popTags();
        Assert.assertEquals(0, tags.size());

    }
}
