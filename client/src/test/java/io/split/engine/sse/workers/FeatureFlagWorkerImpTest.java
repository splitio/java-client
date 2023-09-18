package io.split.engine.sse.workers;

import io.split.client.utils.Json;
import io.split.engine.common.Synchronizer;
import io.split.engine.common.SynchronizerImp;
import io.split.engine.experiments.SplitParser;
import io.split.engine.sse.dtos.FeatureFlagChangeNotification;
import io.split.engine.sse.dtos.GenericNotificationData;
import io.split.engine.sse.dtos.RawMessageNotification;
import io.split.storages.SplitCacheProducer;
import io.split.storages.memory.InMemoryCacheImp;
import io.split.telemetry.domain.UpdatesFromSSE;
import io.split.telemetry.storage.InMemoryTelemetryStorage;
import io.split.telemetry.storage.TelemetryStorage;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashSet;

public class FeatureFlagWorkerImpTest {

    @Test
    public void testRefreshSplitsWithCorrectFF() {
        SplitParser splitParser = new SplitParser();
        Synchronizer synchronizer = Mockito.mock(SynchronizerImp.class);
        SplitCacheProducer splitCacheProducer = Mockito.mock(SplitCacheProducer.class);
        TelemetryStorage telemetryRuntimeProducer = new InMemoryTelemetryStorage();
        FeatureFlagWorkerImp featureFlagsWorker = new FeatureFlagWorkerImp(synchronizer, splitParser, splitCacheProducer, telemetryRuntimeProducer, new HashSet<>());
        String notification = "{\"id\":\"vQQ61wzBRO:0:0\",\"clientId\":\"pri:MTUxNzg3MDg1OQ==\",\"timestamp\":1684265694676,\"encoding\":\"json\",\"channel\":\"NzM2MDI5Mzc0_MjkyNTIzNjczMw==_splits\",\"data\":\"{\\\"type\\\":\\\"SPLIT_UPDATE\\\",\\\"changeNumber\\\":1684265694505,\\\"pcn\\\":0,\\\"c\\\":2,\\\"d\\\":\\\"eJzMk99u2kwQxV8lOtdryQZj8N6hD5QPlThSTVNVEUKDPYZt1jZar1OlyO9emf8lVFWv2ss5zJyd82O8hTWUZSqZvW04opwhUVdsIKBSSKR+10vS1HWW7pIdz2NyBjRwHS8IXEopTLgbQqDYT+ZUm3LxlV4J4mg81LpMyKqygPRc94YeM6eQTtjphp4fegLVXvD6Qdjt9wPXF6gs2bqCxPC/2eRpDIEXpXXblpGuWCDljGptZ4bJ5lxYSJRZBoFkTcWKozpfsoH0goHfCXpB6PfcngDpVQnZEUjKIlOr2uwWqiC3zU5L1aF+3p7LFhUkPv8/mY2nk3gGgZxssmZzb8p6A9n25ktVtA9iGI3ODXunQ3HDp+AVWT6F+rZWlrWq7MN+YkSWWvuTDvkMSnNV7J6oTdl6qKTEvGnmjcCGjL2IYC/ovPYgUKnvvPtbmrmApiVryLM7p2jE++AfH6fTx09/HvuF32LWnNjStM0Xh3c8ukZcsZlEi3h8/zCObsBpJ0acqYLTmFdtqitK1V6NzrfpdPBbLmVx4uK26e27izpDu/r5yf/16AXun2Cr4u6w591xw7+LfDidLj6Mv8TXwP8xbofv/c7UmtHMmx8BAAD//0fclvU=\\\"}\"}";
        RawMessageNotification rawMessageNotification = Json.fromJson(notification, RawMessageNotification.class);
        GenericNotificationData genericNotificationData = Json.fromJson(rawMessageNotification.getData(), GenericNotificationData.class);
        FeatureFlagChangeNotification featureFlagChangeNotification = new FeatureFlagChangeNotification(genericNotificationData);
        featureFlagsWorker.executeRefresh(featureFlagChangeNotification);
        UpdatesFromSSE updatesFromSSE = telemetryRuntimeProducer.popUpdatesFromSSE();
        Assert.assertEquals(1, updatesFromSSE.getSplits());
        Mockito.verify(synchronizer, Mockito.times(0)).refreshSplits(1684265694505L);
        Mockito.verify(synchronizer, Mockito.times(1)).forceRefreshSegment(Mockito.anyString());
    }

    @Test
    public void testRefreshSplitsWithEmptyData() {
        SplitParser splitParser = new SplitParser();
        Synchronizer synchronizer = Mockito.mock(SynchronizerImp.class);
        SplitCacheProducer splitCacheProducer = Mockito.mock(SplitCacheProducer.class);
        TelemetryStorage telemetryRuntimeProducer = new InMemoryTelemetryStorage();
        FeatureFlagWorkerImp featureFlagsWorker = new FeatureFlagWorkerImp(synchronizer, splitParser, splitCacheProducer, telemetryRuntimeProducer, new HashSet<>());
        String notification = "{\"id\":\"vQQ61wzBRO:0:0\",\"clientId\":\"pri:MTUxNzg3MDg1OQ==\",\"timestamp\":1684265694676,\"encoding\":\"json\",\"channel\":\"NzM2MDI5Mzc0_MjkyNTIzNjczMw==_splits\",\"data\":\"{\\\"type\\\":\\\"SPLIT_UPDATE\\\",\\\"changeNumber\\\":1684265694505}\"}";
        RawMessageNotification rawMessageNotification = Json.fromJson(notification, RawMessageNotification.class);
        GenericNotificationData genericNotificationData = Json.fromJson(rawMessageNotification.getData(), GenericNotificationData.class);
        FeatureFlagChangeNotification featureFlagChangeNotification = new FeatureFlagChangeNotification(genericNotificationData);
        featureFlagsWorker.executeRefresh(featureFlagChangeNotification);
        UpdatesFromSSE updatesFromSSE = telemetryRuntimeProducer.popUpdatesFromSSE();
        Assert.assertEquals(0, updatesFromSSE.getSplits());
        Mockito.verify(synchronizer, Mockito.times(1)).refreshSplits(1684265694505L);
        Mockito.verify(synchronizer, Mockito.times(0)).forceRefreshSegment(Mockito.anyString());
    }

    @Test
    public void testRefreshSplitsArchiveFF() {
        SplitParser splitParser = new SplitParser();
        Synchronizer synchronizer = Mockito.mock(SynchronizerImp.class);
        SplitCacheProducer splitCacheProducer = new InMemoryCacheImp(1686165614090L);
        TelemetryStorage telemetryRuntimeProducer = new InMemoryTelemetryStorage();
        FeatureFlagWorkerImp featureFlagsWorker = new FeatureFlagWorkerImp(synchronizer, splitParser, splitCacheProducer, telemetryRuntimeProducer, new HashSet<>());
        String notification = "{\"id\":\"vQQ61wzBRO:0:0\",\"clientId\":\"pri:MTUxNzg3MDg1OQ==\",\"timestamp\":1684265694676,\"encoding\":\"json\",\"channel\":\"NzM2MDI5Mzc0_MjkyNTIzNjczMw==_splits\",\"data\":\"{\\\"type\\\":\\\"SPLIT_UPDATE\\\",\\\"changeNumber\\\":1686165617166,\\\"pcn\\\":1686165614090,\\\"c\\\":2,\\\"d\\\":\\\"eJxsUdFu4jAQ/JVqnx3JDjTh/JZCrj2JBh0EqtOBIuNswKqTIMeuxKH8+ykhiKrqiyXvzM7O7lzAGlEUSqbnEyaiRODgGjRAQOXAIQ/puPB96tHHIPQYQ/QmFNErxEgG44DKnI2AQHXtTOI0my6WcXZAmxoUtsTKvil7nNZVoQ5RYdFERh7VBwK5TY60rqWwqq6AM0q/qa8Qc+As/EHZ5HHMCDR9wQ/9kIajcEygscK6BjhEy+nLr008AwLvSuuOVgjdIIEcC+H03RZw2Hg/n88JEJBHUR0wceUeDXAWTAIWPAYsZEFAQOhDDdwnIPslnOk9NcAvNwEOly3IWtdmC3wLe+1wCy0Q2Hh/zNvTV9xg3sFtr5irQe3v5f7twgAOy8V8vlinQKAUVh7RPJvanbrBsi73qurMQpTM7oSrzjueV6hR2tp05E8J39MV1hq1d7YrWWxsZ2cQGYjzeLXK0pcoyRbLLP69juZZuuiyxoPo2oa7ukqYc+JKNEq+XgVmwopucC6sGMSS9etTvAQCH0I7BO7Ttt21BE7C2E8XsN+l06h/CJy25CveH/eGM0rbHQEt9qiHnR62jtKR7N/8wafQ7tr/AQAA//8S4fPB\\\"}\"}";
        RawMessageNotification rawMessageNotification = Json.fromJson(notification, RawMessageNotification.class);
        GenericNotificationData genericNotificationData = Json.fromJson(rawMessageNotification.getData(), GenericNotificationData.class);
        FeatureFlagChangeNotification featureFlagChangeNotification = new FeatureFlagChangeNotification(genericNotificationData);
        featureFlagsWorker.executeRefresh(featureFlagChangeNotification);
        UpdatesFromSSE updatesFromSSE = telemetryRuntimeProducer.popUpdatesFromSSE();
        Assert.assertEquals(1, updatesFromSSE.getSplits());
        Mockito.verify(synchronizer, Mockito.times(0)).refreshSplits(1686165617166L);
        Mockito.verify(synchronizer, Mockito.times(0)).forceRefreshSegment(Mockito.anyString());
    }
}