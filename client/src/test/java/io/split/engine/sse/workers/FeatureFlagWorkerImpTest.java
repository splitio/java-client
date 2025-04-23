package io.split.engine.sse.workers;

import io.split.client.dtos.ConditionType;
import io.split.client.dtos.Split;
import io.split.client.dtos.RuleBasedSegment;
import io.split.client.dtos.MatcherCombiner;
import io.split.client.interceptors.FlagSetsFilter;
import io.split.client.interceptors.FlagSetsFilterImpl;
import io.split.client.utils.Json;
import io.split.engine.common.Synchronizer;
import io.split.engine.common.SynchronizerImp;
import io.split.engine.experiments.ParsedCondition;
import io.split.engine.experiments.ParsedRuleBasedSegment;
import io.split.engine.experiments.RuleBasedSegmentParser;
import io.split.engine.experiments.SplitParser;
import io.split.engine.matchers.AttributeMatcher;
import io.split.engine.matchers.CombiningMatcher;
import io.split.engine.sse.dtos.CommonChangeNotification;
import io.split.engine.sse.dtos.IncomingNotification;
import io.split.engine.sse.dtos.RawMessageNotification;
import io.split.engine.sse.dtos.GenericNotificationData;
import io.split.storages.RuleBasedSegmentCache;
import io.split.storages.SplitCacheProducer;
import io.split.storages.memory.InMemoryCacheImp;
import io.split.telemetry.domain.UpdatesFromSSE;
import io.split.telemetry.storage.InMemoryTelemetryStorage;
import io.split.telemetry.storage.TelemetryStorage;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import static org.mockito.Mockito.when;

public class FeatureFlagWorkerImpTest {

    private static final FlagSetsFilter FLAG_SETS_FILTER = new FlagSetsFilterImpl(new HashSet<>());

    @Test
    public void testRefreshSplitsWithCorrectFF() {
        SplitParser splitParser = new SplitParser();
        RuleBasedSegmentParser ruleBasedSegmentParser = new RuleBasedSegmentParser();
        Synchronizer synchronizer = Mockito.mock(SynchronizerImp.class);
        SplitCacheProducer splitCacheProducer = Mockito.mock(SplitCacheProducer.class);
        RuleBasedSegmentCache ruleBasedSegmentCache = Mockito.mock(RuleBasedSegmentCache.class);
        TelemetryStorage telemetryRuntimeProducer = new InMemoryTelemetryStorage();
        FeatureFlagWorkerImp featureFlagsWorker = new FeatureFlagWorkerImp(synchronizer, splitParser, ruleBasedSegmentParser, splitCacheProducer, ruleBasedSegmentCache, telemetryRuntimeProducer, FLAG_SETS_FILTER);
        String notification = "{\"id\":\"vQQ61wzBRO:0:0\",\"clientId\":\"pri:MTUxNzg3MDg1OQ==\",\"timestamp\":1684265694676,\"encoding\":\"json\",\"channel\":\"NzM2MDI5Mzc0_MjkyNTIzNjczMw==_splits\",\"data\":\"{\\\"type\\\":\\\"SPLIT_UPDATE\\\",\\\"changeNumber\\\":1684265694505,\\\"pcn\\\":0,\\\"c\\\":2,\\\"d\\\":\\\"eJzMk99u2kwQxV8lOtdryQZj8N6hD5QPlThSTVNVEUKDPYZt1jZar1OlyO9emf8lVFWv2ss5zJyd82O8hTWUZSqZvW04opwhUVdsIKBSSKR+10vS1HWW7pIdz2NyBjRwHS8IXEopTLgbQqDYT+ZUm3LxlV4J4mg81LpMyKqygPRc94YeM6eQTtjphp4fegLVXvD6Qdjt9wPXF6gs2bqCxPC/2eRpDIEXpXXblpGuWCDljGptZ4bJ5lxYSJRZBoFkTcWKozpfsoH0goHfCXpB6PfcngDpVQnZEUjKIlOr2uwWqiC3zU5L1aF+3p7LFhUkPv8/mY2nk3gGgZxssmZzb8p6A9n25ktVtA9iGI3ODXunQ3HDp+AVWT6F+rZWlrWq7MN+YkSWWvuTDvkMSnNV7J6oTdl6qKTEvGnmjcCGjL2IYC/ovPYgUKnvvPtbmrmApiVryLM7p2jE++AfH6fTx09/HvuF32LWnNjStM0Xh3c8ukZcsZlEi3h8/zCObsBpJ0acqYLTmFdtqitK1V6NzrfpdPBbLmVx4uK26e27izpDu/r5yf/16AXun2Cr4u6w591xw7+LfDidLj6Mv8TXwP8xbofv/c7UmtHMmx8BAAD//0fclvU=\\\"}\"}";
        RawMessageNotification rawMessageNotification = Json.fromJson(notification, RawMessageNotification.class);
        GenericNotificationData genericNotificationData = Json.fromJson(rawMessageNotification.getData(), GenericNotificationData.class);

        CommonChangeNotification<Split> featureFlagChangeNotification = new CommonChangeNotification(genericNotificationData, IncomingNotification.Type.SPLIT_UPDATE, Split.class);
        featureFlagsWorker.executeRefresh(featureFlagChangeNotification);
        UpdatesFromSSE updatesFromSSE = telemetryRuntimeProducer.popUpdatesFromSSE();
        Assert.assertEquals(1, updatesFromSSE.getSplits());
        Mockito.verify(synchronizer, Mockito.times(0)).refreshSplits(1684265694505L, 0L);
        Mockito.verify(synchronizer, Mockito.times(1)).forceRefreshSegment(Mockito.anyString());
    }

    @Test
    public void testRefreshSplitsWithEmptyData() {
        SplitParser splitParser = new SplitParser();
        RuleBasedSegmentParser ruleBasedSegmentParser = new RuleBasedSegmentParser();
        Synchronizer synchronizer = Mockito.mock(SynchronizerImp.class);
        SplitCacheProducer splitCacheProducer = Mockito.mock(SplitCacheProducer.class);
        RuleBasedSegmentCache ruleBasedSegmentCache = Mockito.mock(RuleBasedSegmentCache.class);
        TelemetryStorage telemetryRuntimeProducer = new InMemoryTelemetryStorage();
        FeatureFlagWorkerImp featureFlagsWorker = new FeatureFlagWorkerImp(synchronizer, splitParser, ruleBasedSegmentParser, splitCacheProducer, ruleBasedSegmentCache, telemetryRuntimeProducer, FLAG_SETS_FILTER);
        String notification = "{\"id\":\"vQQ61wzBRO:0:0\",\"clientId\":\"pri:MTUxNzg3MDg1OQ==\",\"timestamp\":1684265694676,\"encoding\":\"json\",\"channel\":\"NzM2MDI5Mzc0_MjkyNTIzNjczMw==_splits\",\"data\":\"{\\\"type\\\":\\\"SPLIT_UPDATE\\\",\\\"changeNumber\\\":1684265694505}\"}";
        RawMessageNotification rawMessageNotification = Json.fromJson(notification, RawMessageNotification.class);
        GenericNotificationData genericNotificationData = Json.fromJson(rawMessageNotification.getData(), GenericNotificationData.class);

        CommonChangeNotification<Split> featureFlagChangeNotification = new CommonChangeNotification(genericNotificationData, IncomingNotification.Type.SPLIT_UPDATE, Split.class);
        featureFlagsWorker.executeRefresh(featureFlagChangeNotification);
        UpdatesFromSSE updatesFromSSE = telemetryRuntimeProducer.popUpdatesFromSSE();
        Assert.assertEquals(0, updatesFromSSE.getSplits());
        Mockito.verify(synchronizer, Mockito.times(1)).refreshSplits(1684265694505L, 0L);
        Mockito.verify(synchronizer, Mockito.times(0)).forceRefreshSegment(Mockito.anyString());
    }

    @Test
    public void testRefreshSplitsArchiveFF() {
        SplitParser splitParser = new SplitParser();
        RuleBasedSegmentParser ruleBasedSegmentParser = new RuleBasedSegmentParser();
        Synchronizer synchronizer = Mockito.mock(SynchronizerImp.class);
        SplitCacheProducer splitCacheProducer = new InMemoryCacheImp(1686165614090L, FLAG_SETS_FILTER);
        RuleBasedSegmentCache ruleBasedSegmentCache = Mockito.mock(RuleBasedSegmentCache.class);
        TelemetryStorage telemetryRuntimeProducer = new InMemoryTelemetryStorage();
        FeatureFlagWorkerImp featureFlagsWorker = new FeatureFlagWorkerImp(synchronizer, splitParser, ruleBasedSegmentParser, splitCacheProducer, ruleBasedSegmentCache, telemetryRuntimeProducer, FLAG_SETS_FILTER);
        String notification = "{\"id\":\"vQQ61wzBRO:0:0\",\"clientId\":\"pri:MTUxNzg3MDg1OQ==\",\"timestamp\":1684265694676,\"encoding\":\"json\",\"channel\":\"NzM2MDI5Mzc0_MjkyNTIzNjczMw==_splits\",\"data\":\"{\\\"type\\\":\\\"SPLIT_UPDATE\\\",\\\"changeNumber\\\":1686165617166,\\\"pcn\\\":1686165614090,\\\"c\\\":2,\\\"d\\\":\\\"eJxsUdFu4jAQ/JVqnx3JDjTh/JZCrj2JBh0EqtOBIuNswKqTIMeuxKH8+ykhiKrqiyXvzM7O7lzAGlEUSqbnEyaiRODgGjRAQOXAIQ/puPB96tHHIPQYQ/QmFNErxEgG44DKnI2AQHXtTOI0my6WcXZAmxoUtsTKvil7nNZVoQ5RYdFERh7VBwK5TY60rqWwqq6AM0q/qa8Qc+As/EHZ5HHMCDR9wQ/9kIajcEygscK6BjhEy+nLr008AwLvSuuOVgjdIIEcC+H03RZw2Hg/n88JEJBHUR0wceUeDXAWTAIWPAYsZEFAQOhDDdwnIPslnOk9NcAvNwEOly3IWtdmC3wLe+1wCy0Q2Hh/zNvTV9xg3sFtr5irQe3v5f7twgAOy8V8vlinQKAUVh7RPJvanbrBsi73qurMQpTM7oSrzjueV6hR2tp05E8J39MV1hq1d7YrWWxsZ2cQGYjzeLXK0pcoyRbLLP69juZZuuiyxoPo2oa7ukqYc+JKNEq+XgVmwopucC6sGMSS9etTvAQCH0I7BO7Ttt21BE7C2E8XsN+l06h/CJy25CveH/eGM0rbHQEt9qiHnR62jtKR7N/8wafQ7tr/AQAA//8S4fPB\\\"}\"}";
        RawMessageNotification rawMessageNotification = Json.fromJson(notification, RawMessageNotification.class);
        GenericNotificationData genericNotificationData = Json.fromJson(rawMessageNotification.getData(), GenericNotificationData.class);

        CommonChangeNotification<Split> featureFlagChangeNotification = new CommonChangeNotification(genericNotificationData, IncomingNotification.Type.SPLIT_UPDATE, Split.class);
        featureFlagsWorker.executeRefresh(featureFlagChangeNotification);
        UpdatesFromSSE updatesFromSSE = telemetryRuntimeProducer.popUpdatesFromSSE();
        Assert.assertEquals(1, updatesFromSSE.getSplits());
        Mockito.verify(synchronizer, Mockito.times(0)).refreshSplits(1686165617166L, 0L);
        Mockito.verify(synchronizer, Mockito.times(0)).forceRefreshSegment(Mockito.anyString());
    }

    @Test
    public void testUpdateRuleBasedSegmentsWithCorrectFF() {
        io.split.engine.matchers.Matcher matcher = (matchValue, bucketingKey, attributes, evaluationContext) -> false;
        ParsedCondition parsedCondition = new ParsedCondition(ConditionType.ROLLOUT,
                new CombiningMatcher(MatcherCombiner.AND, Arrays.asList(new AttributeMatcher("email", matcher, false))),
                null,
                "my label");
        ParsedRuleBasedSegment parsedRBS = new ParsedRuleBasedSegment("sample_rule_based_segment",
                                                        Arrays.asList(parsedCondition),
                                                        "user",
                                                        5,
                                                        Arrays.asList("mauro@split.io","gaston@split.io"),
                                                        new ArrayList<>());

        SplitParser splitParser = new SplitParser();
        RuleBasedSegmentParser ruleBasedSegmentParser = new RuleBasedSegmentParser();
        Synchronizer synchronizer = Mockito.mock(SynchronizerImp.class);
        SplitCacheProducer splitCacheProducer = Mockito.mock(SplitCacheProducer.class);
        RuleBasedSegmentCache ruleBasedSegmentCache = Mockito.mock(RuleBasedSegmentCache.class);
        TelemetryStorage telemetryRuntimeProducer = new InMemoryTelemetryStorage();
        FeatureFlagWorkerImp featureFlagsWorker = new FeatureFlagWorkerImp(synchronizer, splitParser, ruleBasedSegmentParser, splitCacheProducer, ruleBasedSegmentCache, telemetryRuntimeProducer, FLAG_SETS_FILTER);
        String notification = "{\"id\":\"vQQ61wzBRO:0:0\",\"clientId\":\"pri:MTUxNzg3MDg1OQ==\",\"timestamp\":1684265694676,\"encoding\":\"json\",\"channel\":\"NzM2MDI5Mzc0_MjkyNTIzNjczMw==_splits\",\"data\":\"{\\\"type\\\":\\\"RB_SEGMENT_UPDATE\\\",\\\"changeNumber\\\":1684265694505,\\\"pcn\\\":0,\\\"c\\\":0,\\\"d\\\":\\\"eyJjaGFuZ2VOdW1iZXIiOiA1LCAibmFtZSI6ICJzYW1wbGVfcnVsZV9iYXNlZF9zZWdtZW50IiwgInN0YXR1cyI6ICJBQ1RJVkUiLCAidHJhZmZpY1R5cGVOYW1lIjogInVzZXIiLCAiZXhjbHVkZWQiOiB7ImtleXMiOiBbIm1hdXJvQHNwbGl0LmlvIiwgImdhc3RvbkBzcGxpdC5pbyJdLCAic2VnbWVudHMiOiBbXX0sICJjb25kaXRpb25zIjogW3sibWF0Y2hlckdyb3VwIjogeyJjb21iaW5lciI6ICJBTkQiLCAibWF0Y2hlcnMiOiBbeyJrZXlTZWxlY3RvciI6IHsidHJhZmZpY1R5cGUiOiAidXNlciIsICJhdHRyaWJ1dGUiOiAiZW1haWwifSwgIm1hdGNoZXJUeXBlIjogIkVORFNfV0lUSCIsICJuZWdhdGUiOiBmYWxzZSwgIndoaXRlbGlzdE1hdGNoZXJEYXRhIjogeyJ3aGl0ZWxpc3QiOiBbIkBzcGxpdC5pbyJdfX1dfX1dfQ==\\\"}\"}";
        RawMessageNotification rawMessageNotification = Json.fromJson(notification, RawMessageNotification.class);
        GenericNotificationData genericNotificationData = Json.fromJson(rawMessageNotification.getData(), GenericNotificationData.class);

        CommonChangeNotification<RuleBasedSegment> ruleBasedSegmentChangeNotification = new CommonChangeNotification(genericNotificationData, IncomingNotification.Type.RB_SEGMENT_UPDATE, RuleBasedSegment.class);
        featureFlagsWorker.executeRefresh(ruleBasedSegmentChangeNotification);
        Mockito.verify(ruleBasedSegmentCache, Mockito.times(1)).update(Arrays.asList(parsedRBS), new ArrayList<>(), 1684265694505L);
    }

    @Test
    public void testRefreshRuleBasedSegmentWithCorrectFF() {
        SplitParser splitParser = new SplitParser();
        RuleBasedSegmentParser ruleBasedSegmentParser = new RuleBasedSegmentParser();
        Synchronizer synchronizer = Mockito.mock(SynchronizerImp.class);
        SplitCacheProducer splitCacheProducer = Mockito.mock(SplitCacheProducer.class);
        RuleBasedSegmentCache ruleBasedSegmentCache = Mockito.mock(RuleBasedSegmentCache.class);
        HashSet<String> rbs = new HashSet<>();
        rbs.add("sample_rule_based_segment");
        when(ruleBasedSegmentCache.contains(rbs)).thenReturn(false);
        TelemetryStorage telemetryRuntimeProducer = new InMemoryTelemetryStorage();
        FeatureFlagWorkerImp featureFlagsWorker = new FeatureFlagWorkerImp(synchronizer, splitParser, ruleBasedSegmentParser, splitCacheProducer, ruleBasedSegmentCache, telemetryRuntimeProducer, FLAG_SETS_FILTER);
        String notification = "{\"id\":\"vQQ61wzBRO:0:0\",\"clientId\":\"pri:MTUxNzg3MDg1OQ==\",\"timestamp\":1684265694676,\"encoding\":\"json\",\"channel\":\"NzM2MDI5Mzc0_MjkyNTIzNjczMw==_splits\",\"data\":\"{\\\"type\\\":\\\"SPLIT_UPDATE\\\",\\\"changeNumber\\\":1684265694505,\\\"pcn\\\":0,\\\"c\\\":0,\\\"d\\\":\\\"eyJjaGFuZ2VOdW1iZXIiOiAxMCwgInRyYWZmaWNUeXBlTmFtZSI6ICJ1c2VyIiwgIm5hbWUiOiAicmJzX2ZsYWciLCAidHJhZmZpY0FsbG9jYXRpb24iOiAxMDAsICJ0cmFmZmljQWxsb2NhdGlvblNlZWQiOiAxODI4Mzc3MzgwLCAic2VlZCI6IC0yODY2MTc5MjEsICJzdGF0dXMiOiAiQUNUSVZFIiwgImtpbGxlZCI6IGZhbHNlLCAiZGVmYXVsdFRyZWF0bWVudCI6ICJvZmYiLCAiYWxnbyI6IDIsICJjb25kaXRpb25zIjogW3siY29uZGl0aW9uVHlwZSI6ICJST0xMT1VUIiwgIm1hdGNoZXJHcm91cCI6IHsiY29tYmluZXIiOiAiQU5EIiwgIm1hdGNoZXJzIjogW3sia2V5U2VsZWN0b3IiOiB7InRyYWZmaWNUeXBlIjogInVzZXIifSwgIm1hdGNoZXJUeXBlIjogIklOX1JVTEVfQkFTRURfU0VHTUVOVCIsICJuZWdhdGUiOiBmYWxzZSwgInVzZXJEZWZpbmVkU2VnbWVudE1hdGNoZXJEYXRhIjogeyJzZWdtZW50TmFtZSI6ICJzYW1wbGVfcnVsZV9iYXNlZF9zZWdtZW50In19XX0sICJwYXJ0aXRpb25zIjogW3sidHJlYXRtZW50IjogIm9uIiwgInNpemUiOiAxMDB9LCB7InRyZWF0bWVudCI6ICJvZmYiLCAic2l6ZSI6IDB9XSwgImxhYmVsIjogImluIHJ1bGUgYmFzZWQgc2VnbWVudCBzYW1wbGVfcnVsZV9iYXNlZF9zZWdtZW50In0sIHsiY29uZGl0aW9uVHlwZSI6ICJST0xMT1VUIiwgIm1hdGNoZXJHcm91cCI6IHsiY29tYmluZXIiOiAiQU5EIiwgIm1hdGNoZXJzIjogW3sia2V5U2VsZWN0b3IiOiB7InRyYWZmaWNUeXBlIjogInVzZXIifSwgIm1hdGNoZXJUeXBlIjogIkFMTF9LRVlTIiwgIm5lZ2F0ZSI6IGZhbHNlfV19LCAicGFydGl0aW9ucyI6IFt7InRyZWF0bWVudCI6ICJvbiIsICJzaXplIjogMH0sIHsidHJlYXRtZW50IjogIm9mZiIsICJzaXplIjogMTAwfV0sICJsYWJlbCI6ICJkZWZhdWx0IHJ1bGUifV0sICJjb25maWd1cmF0aW9ucyI6IHt9LCAic2V0cyI6IFtdLCAiaW1wcmVzc2lvbnNEaXNhYmxlZCI6IGZhbHNlfQ==\\\"}\"}";
        RawMessageNotification rawMessageNotification = Json.fromJson(notification, RawMessageNotification.class);
        GenericNotificationData genericNotificationData = Json.fromJson(rawMessageNotification.getData(), GenericNotificationData.class);
        CommonChangeNotification<Split> featureFlagChangeNotification = new CommonChangeNotification(genericNotificationData, IncomingNotification.Type.SPLIT_UPDATE, Split.class);

        featureFlagsWorker.executeRefresh(featureFlagChangeNotification);
        Mockito.verify(synchronizer, Mockito.times(0)).refreshSplits(0L, 1684265694505L);
    }
}