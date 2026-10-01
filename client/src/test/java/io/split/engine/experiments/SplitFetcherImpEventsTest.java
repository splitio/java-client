package io.split.engine.experiments;

import io.split.client.api.SdkEventType;
import io.split.client.dtos.ChangeDto;
import io.split.client.dtos.RuleBasedSegment;
import io.split.client.dtos.Split;
import io.split.client.dtos.SplitChange;
import io.split.client.dtos.Status;
import io.split.client.interceptors.FlagSetsFilterImpl;
import io.split.client.api.SdkEventMetadata;
import io.split.client.lifecycle.SdkEventsNotifier;
import io.split.client.lifecycle.SdkInternalEvent;
import io.split.engine.common.FetchOptions;
import io.split.storages.RuleBasedSegmentCacheProducer;
import io.split.storages.SplitCacheProducer;
import io.split.telemetry.storage.TelemetryRuntimeProducer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SplitFetcherImpEventsTest {

    private SplitChangeFetcher _changeFetcher;
    private SplitCacheProducer _splitCache;
    private RuleBasedSegmentCacheProducer _rbsCache;
    private SdkEventsNotifier _notifier;
    private SplitFetcherImp _fetcher;

    @Before
    public void setUp() throws Exception {
        _changeFetcher = Mockito.mock(SplitChangeFetcher.class);
        _splitCache = Mockito.mock(SplitCacheProducer.class);
        _rbsCache = Mockito.mock(RuleBasedSegmentCacheProducer.class);
        Mockito.when(_splitCache.getChangeNumber()).thenReturn(-1L);
        Mockito.when(_rbsCache.getChangeNumber()).thenReturn(-1L);
        _notifier = Mockito.mock(SdkEventsNotifier.class);
        _fetcher = new SplitFetcherImp(_changeFetcher, new SplitParser(), _splitCache,
                Mockito.mock(TelemetryRuntimeProducer.class), new FlagSetsFilterImpl(new HashSet<String>()),
                new RuleBasedSegmentParser(), _rbsCache, _notifier);
    }

    @Test
    public void flagChangeEmitsFlagsUpdatedWithExactNames() throws Exception {
        givenChange(flags(activeFlag("f1"), archivedFlag("f2")), rbs());

        _fetcher.forceRefresh(new FetchOptions.Builder().build());

        SdkEventMetadata metadata = verifyEmitted(SdkInternalEvent.FLAGS_UPDATED);
        assertEquals(SdkEventType.FLAGS_UPDATE, metadata.getType());
        assertEquals(new HashSet<>(Arrays.asList("f1", "f2")), metadata.getNames());
    }

    @Test
    public void rbsOnlyChangeEmitsRuleBasedSegmentsUpdatedWithEmptyNames() throws Exception {
        givenChange(flags(), rbs(archivedRbs("r1")));

        _fetcher.forceRefresh(new FetchOptions.Builder().build());

        SdkEventMetadata metadata = verifyEmitted(SdkInternalEvent.RULE_BASED_SEGMENTS_UPDATED);
        assertEquals(SdkEventType.SEGMENTS_UPDATE, metadata.getType());
        assertTrue(metadata.getNames().isEmpty());
    }

    @Test
    public void flagAndRbsChangeEmitsOnlyFlagsUpdated() throws Exception {
        givenChange(flags(activeFlag("f1")), rbs(archivedRbs("r1")));

        _fetcher.forceRefresh(new FetchOptions.Builder().build());

        SdkEventMetadata metadata = verifyEmitted(SdkInternalEvent.FLAGS_UPDATED);
        assertEquals(SdkEventType.FLAGS_UPDATE, metadata.getType());
        assertEquals(Collections.singleton("f1"), metadata.getNames());
    }

    @Test
    public void emptyPollEmitsNothing() throws Exception {
        givenChange(flags(), rbs());

        _fetcher.forceRefresh(new FetchOptions.Builder().build());

        Mockito.verifyZeroInteractions(_notifier);
    }

    @Test
    public void staleChangeEmitsNothing() throws Exception {
        Mockito.when(_splitCache.getChangeNumber()).thenReturn(500L);
        givenChange(flags(activeFlag("f1")), rbs());

        _fetcher.forceRefresh(new FetchOptions.Builder().build());

        Mockito.verifyZeroInteractions(_notifier);
    }

    @Test
    public void defaultConstructorStillWorks() throws Exception {
        givenChange(flags(activeFlag("f1")), rbs());
        SplitFetcherImp plain = new SplitFetcherImp(_changeFetcher, new SplitParser(), _splitCache,
                Mockito.mock(TelemetryRuntimeProducer.class), new FlagSetsFilterImpl(new HashSet<String>()),
                new RuleBasedSegmentParser(), _rbsCache);

        plain.forceRefresh(new FetchOptions.Builder().build());

        Mockito.verifyZeroInteractions(_notifier);
    }

    private SdkEventMetadata verifyEmitted(SdkInternalEvent event) {
        ArgumentCaptor<SdkEventMetadata> captor = ArgumentCaptor.forClass(SdkEventMetadata.class);
        Mockito.verify(_notifier).notify(Mockito.eq(event), captor.capture());
        Mockito.verifyNoMoreInteractions(_notifier);
        return captor.getValue();
    }

    private void givenChange(ChangeDto<Split> ff, ChangeDto<RuleBasedSegment> rbs) throws Exception {
        SplitChange change = new SplitChange();
        change.featureFlags = ff;
        change.ruleBasedSegments = rbs;
        Mockito.when(_changeFetcher.fetch(Mockito.anyLong(), Mockito.anyLong(), Mockito.any(FetchOptions.class)))
                .thenReturn(change);
    }

    private static ChangeDto<Split> flags(Split... splits) {
        ChangeDto<Split> dto = new ChangeDto<>();
        dto.s = -1;
        dto.t = splits.length == 0 ? -1 : 100;
        dto.d = new ArrayList<>(Arrays.asList(splits));
        return dto;
    }

    private static ChangeDto<RuleBasedSegment> rbs(RuleBasedSegment... segments) {
        ChangeDto<RuleBasedSegment> dto = new ChangeDto<>();
        dto.s = -1;
        dto.t = segments.length == 0 ? -1 : 100;
        dto.d = new ArrayList<>(Arrays.asList(segments));
        return dto;
    }

    private static Split activeFlag(String name) {
        Split split = new Split();
        split.name = name;
        split.status = Status.ACTIVE;
        split.defaultTreatment = "off";
        split.trafficTypeName = "user";
        split.trafficAllocation = 100;
        split.trafficAllocationSeed = 1;
        split.algo = 2;
        split.conditions = new ArrayList<>();
        split.prerequisites = new ArrayList<>();
        return split;
    }

    private static Split archivedFlag(String name) {
        Split split = new Split();
        split.name = name;
        split.status = Status.ARCHIVED;
        return split;
    }

    private static RuleBasedSegment archivedRbs(String name) {
        RuleBasedSegment segment = new RuleBasedSegment();
        segment.name = name;
        segment.status = Status.ARCHIVED;
        return segment;
    }
}
