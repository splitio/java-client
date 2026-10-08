package io.split.engine.sse.workers;

import io.split.client.api.SdkEventMetadata;
import io.split.client.api.SdkEventType;
import io.split.client.dtos.RuleBasedSegment;
import io.split.client.dtos.Split;
import io.split.client.interceptors.FlagSetsFilter;
import io.split.client.interceptors.FlagSetsFilterImpl;
import io.split.client.lifecycle.SdkEventsNotifier;
import io.split.client.lifecycle.SdkInternalEvent;
import io.split.client.utils.Json;
import io.split.engine.common.Synchronizer;
import io.split.engine.common.SynchronizerImp;
import io.split.engine.experiments.RuleBasedSegmentParser;
import io.split.engine.experiments.SplitParser;
import io.split.engine.sse.dtos.CommonChangeNotification;
import io.split.engine.sse.dtos.GenericNotificationData;
import io.split.engine.sse.dtos.IncomingNotification;
import io.split.engine.sse.dtos.RawMessageNotification;
import io.split.storages.RuleBasedSegmentCache;
import io.split.storages.SplitCacheProducer;
import io.split.telemetry.storage.InMemoryTelemetryStorage;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FeatureFlagWorkerImpEventsTest {

    private static final FlagSetsFilter FLAG_SETS_FILTER = new FlagSetsFilterImpl(new HashSet<>());

    private static final String WITH_DEFINITION = "{\"id\":\"vQQ61wzBRO:0:0\",\"clientId\":\"pri:MTUxNzg3MDg1OQ==\",\"timestamp\":1684265694676,\"encoding\":\"json\",\"channel\":\"NzM2MDI5Mzc0_MjkyNTIzNjczMw==_splits\",\"data\":\"{\\\"type\\\":\\\"SPLIT_UPDATE\\\",\\\"changeNumber\\\":1684265694505,\\\"pcn\\\":0,\\\"c\\\":2,\\\"d\\\":\\\"eJzMk99u2kwQxV8lOtdryQZj8N6hD5QPlThSTVNVEUKDPYZt1jZar1OlyO9emf8lVFWv2ss5zJyd82O8hTWUZSqZvW04opwhUVdsIKBSSKR+10vS1HWW7pIdz2NyBjRwHS8IXEopTLgbQqDYT+ZUm3LxlV4J4mg81LpMyKqygPRc94YeM6eQTtjphp4fegLVXvD6Qdjt9wPXF6gs2bqCxPC/2eRpDIEXpXXblpGuWCDljGptZ4bJ5lxYSJRZBoFkTcWKozpfsoH0goHfCXpB6PfcngDpVQnZEUjKIlOr2uwWqiC3zU5L1aF+3p7LFhUkPv8/mY2nk3gGgZxssmZzb8p6A9n25ktVtA9iGI3ODXunQ3HDp+AVWT6F+rZWlrWq7MN+YkSWWvuTDvkMSnNV7J6oTdl6qKTEvGnmjcCGjL2IYC/ovPYgUKnvvPtbmrmApiVryLM7p2jE++AfH6fTx09/HvuF32LWnNjStM0Xh3c8ukZcsZlEi3h8/zCObsBpJ0acqYLTmFdtqitK1V6NzrfpdPBbLmVx4uK26e27izpDu/r5yf/16AXun2Cr4u6w591xw7+LfDidLj6Mv8TXwP8xbofv/c7UmtHMmx8BAAD//0fclvU=\\\"}\"}";

    private static final String WITHOUT_DEFINITION = "{\"id\":\"vQQ61wzBRO:0:0\",\"clientId\":\"pri:MTUxNzg3MDg1OQ==\",\"timestamp\":1684265694676,\"encoding\":\"json\",\"channel\":\"NzM2MDI5Mzc0_MjkyNTIzNjczMw==_splits\",\"data\":\"{\\\"type\\\":\\\"SPLIT_UPDATE\\\",\\\"changeNumber\\\":1684265694505}\"}";

    private static final String RBS_WITH_DEFINITION = "{\"id\":\"vQQ61wzBRO:0:0\",\"clientId\":\"pri:MTUxNzg3MDg1OQ==\",\"timestamp\":1684265694676,\"encoding\":\"json\",\"channel\":\"NzM2MDI5Mzc0_MjkyNTIzNjczMw==_splits\",\"data\":\"{\\\"type\\\":\\\"RB_SEGMENT_UPDATE\\\",\\\"changeNumber\\\":1684265694505,\\\"pcn\\\":0,\\\"c\\\":0,\\\"d\\\":\\\"eyJjaGFuZ2VOdW1iZXIiOiA1LCAibmFtZSI6ICJzYW1wbGVfcnVsZV9iYXNlZF9zZWdtZW50IiwgInN0YXR1cyI6ICJBQ1RJVkUiLCAidHJhZmZpY1R5cGVOYW1lIjogInVzZXIiLCAiZXhjbHVkZWQiOiB7ImtleXMiOiBbIm1hdXJvQHNwbGl0LmlvIiwgImdhc3RvbkBzcGxpdC5pbyJdLCAic2VnbWVudHMiOiBbXX0sICJjb25kaXRpb25zIjogW3sibWF0Y2hlckdyb3VwIjogeyJjb21iaW5lciI6ICJBTkQiLCAibWF0Y2hlcnMiOiBbeyJrZXlTZWxlY3RvciI6IHsidHJhZmZpY1R5cGUiOiAidXNlciIsICJhdHRyaWJ1dGUiOiAiZW1haWwifSwgIm1hdGNoZXJUeXBlIjogIkVORFNfV0lUSCIsICJuZWdhdGUiOiBmYWxzZSwgIndoaXRlbGlzdE1hdGNoZXJEYXRhIjogeyJ3aGl0ZWxpc3QiOiBbIkBzcGxpdC5pbyJdfX1dfX1dfQ==\\\"}\"}";

    private static final String RBS_WITHOUT_DEFINITION = "{\"id\":\"vQQ61wzBRO:0:0\",\"clientId\":\"pri:MTUxNzg3MDg1OQ==\",\"timestamp\":1684265694676,\"encoding\":\"json\",\"channel\":\"NzM2MDI5Mzc0_MjkyNTIzNjczMw==_splits\",\"data\":\"{\\\"type\\\":\\\"RB_SEGMENT_UPDATE\\\",\\\"changeNumber\\\":1684265694505}\"}";

    private static final class Recorded {
        final SdkInternalEvent event;
        final SdkEventMetadata metadata;

        Recorded(SdkInternalEvent event, SdkEventMetadata metadata) {
            this.event = event;
            this.metadata = metadata;
        }
    }

    private static List<Recorded> refresh(String notification) {
        return refresh(notification, 0L, Split.class);
    }

    private static List<Recorded> refreshRuleBasedSegment(String notification, long cachedChangeNumber) {
        return refresh(notification, cachedChangeNumber, RuleBasedSegment.class);
    }

    private static List<Recorded> refresh(String notification, long rbsCachedChangeNumber, Class<?> definitionType) {
        List<Recorded> recorded = new ArrayList<>();
        SdkEventsNotifier notifier = (event, metadata) -> recorded.add(new Recorded(event, metadata));
        Synchronizer synchronizer = Mockito.mock(SynchronizerImp.class);
        SplitCacheProducer splitCacheProducer = Mockito.mock(SplitCacheProducer.class);
        RuleBasedSegmentCache ruleBasedSegmentCache = Mockito.mock(RuleBasedSegmentCache.class);
        Mockito.when(ruleBasedSegmentCache.getChangeNumber()).thenReturn(rbsCachedChangeNumber);
        FeatureFlagWorkerImp worker = new FeatureFlagWorkerImp(synchronizer, new SplitParser(), new RuleBasedSegmentParser(),
                splitCacheProducer, ruleBasedSegmentCache, new InMemoryTelemetryStorage(), FLAG_SETS_FILTER, notifier);

        RawMessageNotification raw = Json.fromJson(notification, RawMessageNotification.class);
        GenericNotificationData data = Json.fromJson(raw.getData(), GenericNotificationData.class);
        worker.executeRefresh((IncomingNotification) new CommonChangeNotification(data, definitionType));
        return recorded;
    }

    @Test
    public void aFlagDefinitionPushedOverSseRaisesOneFlagsUpdated() {
        List<Recorded> recorded = refresh(WITH_DEFINITION);

        assertEquals(1, recorded.size());
        assertEquals(SdkInternalEvent.FLAGS_UPDATED, recorded.get(0).event);
        assertEquals(SdkEventType.FLAGS_UPDATE, recorded.get(0).metadata.getType());
        assertEquals(1, recorded.get(0).metadata.getNames().size());
    }

    @Test
    public void aNotificationWithoutDefinitionRaisesNothingItself() {
        // The worker falls back to refreshSplits(); the fetcher is then the one that emits.
        assertTrue(refresh(WITHOUT_DEFINITION).isEmpty());
    }

    @Test
    public void aRuleBasedSegmentDefinitionPushedOverSseRaisesOneRuleBasedSegmentsUpdated() {
        List<Recorded> recorded = refreshRuleBasedSegment(RBS_WITH_DEFINITION, 0L);

        assertEquals(1, recorded.size());
        assertEquals(SdkInternalEvent.RULE_BASED_SEGMENTS_UPDATED, recorded.get(0).event);
        assertEquals(SdkEventType.SEGMENTS_UPDATE, recorded.get(0).metadata.getType());
        assertTrue(recorded.get(0).metadata.getNames().isEmpty());
    }

    @Test
    public void aRuleBasedSegmentNotificationWithoutDefinitionRaisesNothingItself() {
        assertTrue(refreshRuleBasedSegment(RBS_WITHOUT_DEFINITION, 0L).isEmpty());
    }

    @Test
    public void aStaleRuleBasedSegmentNotificationRaisesNothing() {
        assertTrue(refreshRuleBasedSegment(RBS_WITH_DEFINITION, Long.MAX_VALUE).isEmpty());
    }
}
