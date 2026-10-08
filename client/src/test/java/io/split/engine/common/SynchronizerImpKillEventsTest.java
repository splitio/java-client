package io.split.engine.common;

import io.split.client.api.SdkEventType;
import io.split.client.events.EventsTask;
import io.split.client.impressions.ImpressionsManager;
import io.split.client.impressions.UniqueKeysTracker;
import io.split.client.api.SdkEventMetadata;
import io.split.client.lifecycle.SdkEventsNotifier;
import io.split.client.lifecycle.SdkInternalEvent;
import io.split.engine.experiments.FetchResult;
import io.split.engine.experiments.SplitFetcher;
import io.split.engine.experiments.SplitSynchronizationTask;
import io.split.engine.segments.SegmentSynchronizationTask;
import io.split.engine.sse.dtos.SplitKillNotification;
import io.split.storages.RuleBasedSegmentCache;
import io.split.storages.SegmentCache;
import io.split.storages.SplitCache;
import io.split.telemetry.synchronizer.TelemetrySyncTask;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;

public class SynchronizerImpKillEventsTest {

    private SplitCache _splitCache;
    private SdkEventsNotifier _notifier;
    private SynchronizerImp _synchronizer;

    @Before
    public void setUp() {
        SplitFetcher splitFetcher = Mockito.mock(SplitFetcher.class);
        Mockito.when(splitFetcher.forceRefresh(Mockito.any(FetchOptions.class)))
                .thenReturn(new FetchResult(true, false, new HashSet<String>()));
        _splitCache = Mockito.mock(SplitCache.class);
        // first read is localKillSplit's staleness check; afterwards the kill has "landed" so refreshSplits returns immediately
        Mockito.when(_splitCache.getChangeNumber()).thenReturn(10L, 20L);
        RuleBasedSegmentCache rbsCache = Mockito.mock(RuleBasedSegmentCache.class);
        SplitTasks tasks = SplitTasks.build(Mockito.mock(SplitSynchronizationTask.class),
                Mockito.mock(SegmentSynchronizationTask.class), Mockito.mock(ImpressionsManager.class),
                Mockito.mock(EventsTask.class), Mockito.mock(TelemetrySyncTask.class), Mockito.mock(UniqueKeysTracker.class));
        _notifier = Mockito.mock(SdkEventsNotifier.class);
        _synchronizer = new SynchronizerImp(tasks, splitFetcher, _splitCache, Mockito.mock(SegmentCache.class), rbsCache,
                50, 10, 5, new HashSet<String>(), _notifier);
    }

    @Test
    public void killEmitsFlagKilledNotificationWithFlagName() {
        _synchronizer.localKillSplit(kill("flag1", 20L));

        ArgumentCaptor<SdkEventMetadata> captor = ArgumentCaptor.forClass(SdkEventMetadata.class);
        Mockito.verify(_notifier).notify(Mockito.eq(SdkInternalEvent.FLAG_KILLED_NOTIFICATION), captor.capture());
        Mockito.verifyNoMoreInteractions(_notifier);
        assertEquals(SdkEventType.FLAGS_UPDATE, captor.getValue().getType());
        assertEquals(Collections.singleton("flag1"), captor.getValue().getNames());
    }

    @Test
    public void staleKillEmitsNothing() {
        _synchronizer.localKillSplit(kill("flag1", 5L));

        Mockito.verifyZeroInteractions(_notifier);
    }

    private static SplitKillNotification kill(String name, long changeNumber) {
        SplitKillNotification notification = Mockito.mock(SplitKillNotification.class);
        Mockito.when(notification.getSplitName()).thenReturn(name);
        Mockito.when(notification.getChangeNumber()).thenReturn(changeNumber);
        Mockito.when(notification.getDefaultTreatment()).thenReturn("off");
        return notification;
    }
}
