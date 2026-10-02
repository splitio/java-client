package io.split.engine.common;

import io.split.client.SplitClientConfig;
import io.split.client.api.SdkEventMetadata;
import io.split.client.events.EventsTask;
import io.split.client.impressions.ImpressionsManagerImpl;
import io.split.client.impressions.UniqueKeysTracker;
import io.split.client.lifecycle.SdkEventsNotifier;
import io.split.client.lifecycle.SdkInternalEvent;
import io.split.engine.SDKReadinessGates;
import io.split.engine.experiments.SplitSynchronizationTask;
import io.split.engine.segments.SegmentSynchronizationTaskImp;
import io.split.telemetry.storage.TelemetryStorage;
import io.split.telemetry.synchronizer.TelemetrySyncTask;
import io.split.telemetry.synchronizer.TelemetrySynchronizer;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.LinkedBlockingQueue;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class SyncManagerReadyEventsTest {

    private static void assertReadyMetadata(SdkEventsNotifier notifier) {
        ArgumentCaptor<SdkEventMetadata> metadata = ArgumentCaptor.forClass(SdkEventMetadata.class);
        verify(notifier, timeout(2000).times(1)).notify(eq(SdkInternalEvent.SDK_READY), metadata.capture());
        assertTrue(metadata.getValue().isInitialCacheLoad());
        assertNull(metadata.getValue().getLastUpdateTimestamp());
    }

    @Test
    public void standaloneSyncManagerNotifiesSdkReadyOnceAfterSyncAll() throws Exception {
        Synchronizer synchronizer = mock(Synchronizer.class);
        when(synchronizer.syncAll()).thenReturn(true);
        SDKReadinessGates gates = new SDKReadinessGates();
        SdkEventsNotifier notifier = mock(SdkEventsNotifier.class);
        SplitTasks tasks = SplitTasks.build(mock(SplitSynchronizationTask.class), mock(SegmentSynchronizationTaskImp.class),
                mock(ImpressionsManagerImpl.class), mock(EventsTask.class), mock(TelemetrySyncTask.class),
                mock(UniqueKeysTracker.class));
        SyncManagerImp syncManager = new SyncManagerImp(tasks, false, synchronizer, mock(PushManager.class),
                new LinkedBlockingQueue<>(), gates, mock(TelemetryStorage.class), mock(TelemetrySynchronizer.class),
                SplitClientConfig.builder().build(), mock(SplitAPI.class), notifier);

        syncManager.start();

        assertReadyMetadata(notifier);
        assertTrue(gates.isSDKReady());
        syncManager.shutdown();
    }

    @Test
    public void standaloneSyncManagerDoesNotNotifyWhenSyncAllNeverSucceeds() throws Exception {
        Synchronizer synchronizer = mock(Synchronizer.class);
        when(synchronizer.syncAll()).thenReturn(false);
        SdkEventsNotifier notifier = mock(SdkEventsNotifier.class);
        SplitTasks tasks = SplitTasks.build(mock(SplitSynchronizationTask.class), mock(SegmentSynchronizationTaskImp.class),
                mock(ImpressionsManagerImpl.class), mock(EventsTask.class), mock(TelemetrySyncTask.class),
                mock(UniqueKeysTracker.class));
        SyncManagerImp syncManager = new SyncManagerImp(tasks, false, synchronizer, mock(PushManager.class),
                new LinkedBlockingQueue<>(), new SDKReadinessGates(), mock(TelemetryStorage.class),
                mock(TelemetrySynchronizer.class), SplitClientConfig.builder().build(), mock(SplitAPI.class), notifier);

        syncManager.start();
        Thread.sleep(300);

        verify(notifier, never()).notify(any(SdkInternalEvent.class), any(SdkEventMetadata.class));
        syncManager.shutdown();
    }

    @Test
    public void localhostSyncManagerNotifiesSdkReadyAfterGate() {
        Synchronizer synchronizer = mock(Synchronizer.class);
        when(synchronizer.syncAll()).thenReturn(true);
        SDKReadinessGates gates = new SDKReadinessGates();
        SdkEventsNotifier notifier = mock(SdkEventsNotifier.class);

        new LocalhostSyncManager(synchronizer, gates, notifier).start();

        assertTrue(gates.isSDKReady());
        assertReadyMetadata(notifier);
    }

    @Test
    public void localhostSyncManagerDoesNotNotifyWhenSyncFails() {
        Synchronizer synchronizer = mock(Synchronizer.class);
        when(synchronizer.syncAll()).thenReturn(false);
        SdkEventsNotifier notifier = mock(SdkEventsNotifier.class);

        new LocalhostSyncManager(synchronizer, new SDKReadinessGates(), notifier).start();

        verifyZeroInteractions(notifier);
    }
}
