package io.split.engine.segments;

import io.split.client.api.SdkEventType;
import io.split.client.dtos.SegmentChange;
import io.split.client.api.SdkEventMetadata;
import io.split.client.lifecycle.SdkEventsNotifier;
import io.split.client.lifecycle.SdkInternalEvent;
import io.split.engine.common.FetchOptions;
import io.split.storages.memory.SegmentCacheInMemoryImpl;
import io.split.telemetry.storage.TelemetryRuntimeProducer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SegmentFetcherImpEventsTest {

    private SegmentChangeFetcher _changeFetcher;
    private SdkEventsNotifier _notifier;
    private SegmentFetcherImp _fetcher;

    @Before
    public void setUp() {
        _changeFetcher = Mockito.mock(SegmentChangeFetcher.class);
        _notifier = Mockito.mock(SdkEventsNotifier.class);
        _fetcher = new SegmentFetcherImp("seg", _changeFetcher, new SegmentCacheInMemoryImpl(),
                Mockito.mock(TelemetryRuntimeProducer.class), _notifier);
    }

    @Test
    public void constructionAloneEmitsNothing() {
        Mockito.verifyZeroInteractions(_notifier);
    }

    @Test
    public void membershipChangeEmitsSegmentsUpdatedWithEmptyNames() {
        Mockito.when(_changeFetcher.fetch(Mockito.eq("seg"), Mockito.anyLong(), Mockito.any(FetchOptions.class)))
                .thenReturn(change(-1, 10, Arrays.asList("a", "b"), new ArrayList<String>()));

        _fetcher.fetch(new FetchOptions.Builder().build());

        ArgumentCaptor<SdkEventMetadata> captor = ArgumentCaptor.forClass(SdkEventMetadata.class);
        Mockito.verify(_notifier).notify(Mockito.eq(SdkInternalEvent.SEGMENTS_UPDATED), captor.capture());
        Mockito.verifyNoMoreInteractions(_notifier);
        assertEquals(SdkEventType.SEGMENTS_UPDATE, captor.getValue().getType());
        assertTrue(captor.getValue().getNames().isEmpty());
    }

    @Test
    public void noChangeEmitsNothing() {
        Mockito.when(_changeFetcher.fetch(Mockito.eq("seg"), Mockito.anyLong(), Mockito.any(FetchOptions.class)))
                .thenReturn(change(-1, 10, new ArrayList<String>(), new ArrayList<String>()));

        _fetcher.fetch(new FetchOptions.Builder().build());

        Mockito.verifyZeroInteractions(_notifier);
    }

    private static SegmentChange change(long since, long till, java.util.List<String> added, java.util.List<String> removed) {
        SegmentChange change = new SegmentChange();
        change.name = "seg";
        change.since = since;
        change.till = till;
        change.added = added;
        change.removed = removed;
        return change;
    }
}
