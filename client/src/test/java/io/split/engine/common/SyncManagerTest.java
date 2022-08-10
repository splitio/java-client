package io.split.engine.common;

import io.split.client.SplitClientConfig;
import io.split.client.events.EventsTask;
import io.split.client.impressions.ImpressionsManager;
import io.split.engine.SDKReadinessGates;
import io.split.telemetry.storage.InMemoryTelemetryStorage;
import io.split.telemetry.storage.TelemetryStorage;
import io.split.telemetry.synchronizer.TelemetrySyncTask;
import io.split.telemetry.synchronizer.TelemetrySynchronizer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.concurrent.LinkedBlockingQueue;

public class SyncManagerTest {
    private Synchronizer _synchronizer;
    private PushManager _pushManager;
    private SDKReadinessGates _gates;

    @Before
    public void setUp() {
        _synchronizer = Mockito.mock(Synchronizer.class);
        _pushManager = Mockito.mock(PushManager.class);
        _gates = Mockito.mock(SDKReadinessGates.class);
    }

    @Test
    public void startWithStreamingFalseShouldStartPolling() throws InterruptedException {
        TelemetryStorage telemetryStorage = Mockito.mock(TelemetryStorage.class);
        _gates.sdkInternalReady();
        TelemetrySynchronizer telemetrySynchronizer = Mockito.mock(TelemetrySynchronizer.class);
        ImpressionsManager impressionsManager = Mockito.mock(ImpressionsManager.class);
        EventsTask eventsTask = Mockito.mock(EventsTask.class);
        SplitClientConfig config = Mockito.mock(SplitClientConfig.class);
        TelemetrySyncTask telemetrySyncTask = Mockito.mock(TelemetrySyncTask.class);
        Mockito.when(_synchronizer.syncAll()).thenReturn(true);

        SplitTasks splitTasks = SplitTasks.build(null, null,
                impressionsManager, eventsTask, telemetrySyncTask);
        SyncManagerImp syncManager = new SyncManagerImp( splitTasks, false, _synchronizer, _pushManager, new LinkedBlockingQueue<>(),
                _gates, telemetryStorage, telemetrySynchronizer, config);
        syncManager.start();
        Thread.sleep(1000);
        Mockito.verify(_synchronizer, Mockito.times(1)).startPeriodicFetching();
        Mockito.verify(_synchronizer, Mockito.times(1)).syncAll();
        Mockito.verify(_pushManager, Mockito.times(0)).start();
    }

    @Test
    public void startWithStreamingTrueShouldStartSyncAll() throws InterruptedException {
        TelemetryStorage telemetryStorage = Mockito.mock(TelemetryStorage.class);
        TelemetrySynchronizer telemetrySynchronizer = Mockito.mock(TelemetrySynchronizer.class);
        ImpressionsManager impressionsManager = Mockito.mock(ImpressionsManager.class);
        EventsTask eventsTask = Mockito.mock(EventsTask.class);
        SplitClientConfig config = Mockito.mock(SplitClientConfig.class);
        TelemetrySyncTask telemetrySyncTask = Mockito.mock(TelemetrySyncTask.class);

        SplitTasks splitTasks = SplitTasks.build(null, null,
                impressionsManager, eventsTask, telemetrySyncTask);
        Mockito.when(_synchronizer.syncAll()).thenReturn(true);
        SyncManager sm = new SyncManagerImp(splitTasks, true, _synchronizer, _pushManager, new LinkedBlockingQueue<>(),
                _gates, telemetryStorage, telemetrySynchronizer, config);
        sm.start();
        Thread.sleep(1000);
        Mockito.verify(_synchronizer, Mockito.times(0)).startPeriodicFetching();
        Mockito.verify(_synchronizer, Mockito.times(1)).syncAll();
        Mockito.verify(_pushManager, Mockito.times(1)).start();
        Mockito.verify(telemetryStorage, Mockito.times(1)).recordStreamingEvents(Mockito.any());
    }

    @Test
    public void onStreamingAvailable() throws InterruptedException {
        TelemetryStorage telemetryStorage = Mockito.mock(TelemetryStorage.class);
        LinkedBlockingQueue<PushManager.Status> messages = new LinkedBlockingQueue<>();
        TelemetrySynchronizer telemetrySynchronizer = Mockito.mock(TelemetrySynchronizer.class);
        ImpressionsManager impressionsManager = Mockito.mock(ImpressionsManager.class);
        TelemetrySyncTask telemetrySyncTask = Mockito.mock(TelemetrySyncTask.class);
        EventsTask eventsTask = Mockito.mock(EventsTask.class);
        SplitClientConfig config = Mockito.mock(SplitClientConfig.class);
        SplitTasks splitTasks = SplitTasks.build(null, null,
                impressionsManager, eventsTask, telemetrySyncTask);

        SyncManagerImp syncManager = new SyncManagerImp(splitTasks, true, _synchronizer, _pushManager, messages,
                _gates, telemetryStorage, telemetrySynchronizer, config);
        Thread t = new Thread(syncManager::incomingPushStatusHandler);
        t.start();
        messages.offer(PushManager.Status.STREAMING_READY);
        Thread.sleep(500);
        Mockito.verify(_synchronizer, Mockito.times(1)).stopPeriodicFetching();
        Mockito.verify(_synchronizer, Mockito.times(1)).syncAll();
        Mockito.verify(_pushManager, Mockito.times(1)).startWorkers();
        Mockito.verify(telemetryStorage, Mockito.times(1)).recordStreamingEvents(Mockito.any());
        t.interrupt();
    }

    @Test
    public void onStreamingDisabled() throws InterruptedException {
        TelemetryStorage telemetryStorage = new InMemoryTelemetryStorage();
        LinkedBlockingQueue<PushManager.Status> messsages = new LinkedBlockingQueue<>();
        TelemetrySynchronizer telemetrySynchronizer = Mockito.mock(TelemetrySynchronizer.class);
        ImpressionsManager impressionsManager = Mockito.mock(ImpressionsManager.class);
        TelemetrySyncTask telemetrySyncTask = Mockito.mock(TelemetrySyncTask.class);
        EventsTask eventsTask = Mockito.mock(EventsTask.class);
        SplitClientConfig config = Mockito.mock(SplitClientConfig.class);
        SplitTasks splitTasks = SplitTasks.build(null, null,
                impressionsManager, eventsTask, telemetrySyncTask);

        SyncManagerImp syncManager = new SyncManagerImp(splitTasks, true, _synchronizer, _pushManager, messsages,
                _gates, telemetryStorage, telemetrySynchronizer, config);
        Thread t = new Thread(syncManager::incomingPushStatusHandler);
        t.start();
        messsages.offer(PushManager.Status.STREAMING_DOWN);
        Thread.sleep(500);

        Mockito.verify(_synchronizer, Mockito.times(1)).startPeriodicFetching();
        Mockito.verify(_pushManager, Mockito.times(1)).stopWorkers();
        t.interrupt();
    }

    @Test
    public void onStreamingShutdown() throws InterruptedException {
        TelemetryStorage telemetryStorage = new InMemoryTelemetryStorage();
        LinkedBlockingQueue<PushManager.Status> messsages = new LinkedBlockingQueue<>();
        TelemetrySynchronizer telemetrySynchronizer = Mockito.mock(TelemetrySynchronizer.class);
        ImpressionsManager impressionsManager = Mockito.mock(ImpressionsManager.class);
        TelemetrySyncTask telemetrySyncTask = Mockito.mock(TelemetrySyncTask.class);
        EventsTask eventsTask = Mockito.mock(EventsTask.class);
        SplitClientConfig config = Mockito.mock(SplitClientConfig.class);
        SplitTasks splitTasks = SplitTasks.build(null, null,
                impressionsManager, eventsTask, telemetrySyncTask);

        SyncManagerImp syncManager = new SyncManagerImp(splitTasks, true, _synchronizer, _pushManager, messsages,
                _gates, telemetryStorage, telemetrySynchronizer, config);
        Thread t = new Thread(syncManager::incomingPushStatusHandler);
        t.start();
        messsages.offer(PushManager.Status.STREAMING_OFF);
        Thread.sleep(500);
        Mockito.verify(_pushManager, Mockito.times(1)).stop();
        t.interrupt();
    }

    @Test
    public void onConnected() throws InterruptedException {
        TelemetryStorage telemetryStorage = new InMemoryTelemetryStorage();
        LinkedBlockingQueue<PushManager.Status> messsages = new LinkedBlockingQueue<>();
        TelemetrySynchronizer telemetrySynchronizer = Mockito.mock(TelemetrySynchronizer.class);
        ImpressionsManager impressionsManager = Mockito.mock(ImpressionsManager.class);
        TelemetrySyncTask telemetrySyncTask = Mockito.mock(TelemetrySyncTask.class);
        EventsTask eventsTask = Mockito.mock(EventsTask.class);
        SplitClientConfig config = Mockito.mock(SplitClientConfig.class);
        SplitTasks splitTasks = SplitTasks.build(null, null,
                impressionsManager, eventsTask, telemetrySyncTask);

        SyncManagerImp syncManager = new SyncManagerImp(splitTasks, true, _synchronizer, _pushManager, messsages,
                _gates, telemetryStorage, telemetrySynchronizer, config);
        Thread t = new Thread(syncManager::incomingPushStatusHandler);
        t.start();
        messsages.offer(PushManager.Status.STREAMING_READY);
        Thread.sleep(500);
        Mockito.verify(_synchronizer, Mockito.times(1)).stopPeriodicFetching();
        Mockito.verify(_synchronizer, Mockito.times(1)).syncAll();
        t.interrupt();
    }

    @Test
    public void onDisconnect() throws InterruptedException {
        TelemetryStorage telemetryStorage = new InMemoryTelemetryStorage();
        LinkedBlockingQueue<PushManager.Status> messsages = new LinkedBlockingQueue<>();
        TelemetrySynchronizer telemetrySynchronizer = Mockito.mock(TelemetrySynchronizer.class);
        ImpressionsManager impressionsManager = Mockito.mock(ImpressionsManager.class);
        TelemetrySyncTask telemetrySyncTask = Mockito.mock(TelemetrySyncTask.class);
        EventsTask eventsTask = Mockito.mock(EventsTask.class);
        SplitClientConfig config = Mockito.mock(SplitClientConfig.class);
        SplitTasks splitTasks = SplitTasks.build(null, null,
                impressionsManager, eventsTask, telemetrySyncTask);

        SyncManagerImp syncManager = new SyncManagerImp(splitTasks, true, _synchronizer, _pushManager, messsages,
                _gates, telemetryStorage, telemetrySynchronizer, config);
        Thread t = new Thread(syncManager::incomingPushStatusHandler);
        t.start();
        messsages.offer(PushManager.Status.STREAMING_OFF);
        Thread.sleep(500);
        Mockito.verify(_synchronizer, Mockito.times(1)).startPeriodicFetching();
        t.interrupt();
    }

    @Test
    public void onDisconnectAndReconnect() throws InterruptedException { // Check with mauro. reconnect should call pushManager.start again, right?
        TelemetryStorage telemetryStorage = new InMemoryTelemetryStorage();
        LinkedBlockingQueue<PushManager.Status> messsages = new LinkedBlockingQueue<>();
        TelemetrySynchronizer telemetrySynchronizer = Mockito.mock(TelemetrySynchronizer.class);
        ImpressionsManager impressionsManager = Mockito.mock(ImpressionsManager.class);
        TelemetrySyncTask telemetrySyncTask = Mockito.mock(TelemetrySyncTask.class);
        EventsTask eventsTask = Mockito.mock(EventsTask.class);
        SplitClientConfig config = Mockito.mock(SplitClientConfig.class);

        SplitTasks splitTasks = SplitTasks.build(null, null,
                impressionsManager, eventsTask, telemetrySyncTask);
        Mockito.when(_synchronizer.syncAll()).thenReturn(true);
        SyncManagerImp syncManager = new SyncManagerImp(splitTasks, true, _synchronizer, _pushManager, messsages,
                _gates, telemetryStorage, telemetrySynchronizer, config);
        syncManager.start();
        messsages.offer(PushManager.Status.STREAMING_BACKOFF);
        Thread.sleep(1200);
        Mockito.verify(_synchronizer, Mockito.times(1)).startPeriodicFetching();
        Mockito.verify(_synchronizer, Mockito.times(1)).syncAll();
        Mockito.verify(_pushManager, Mockito.times(2)).start();
    }

    @Test
    public void syncAllRetryThenShouldStartPolling() throws InterruptedException {
        TelemetryStorage telemetryStorage = new InMemoryTelemetryStorage();
        TelemetrySynchronizer telemetrySynchronizer = Mockito.mock(TelemetrySynchronizer.class);
        ImpressionsManager impressionsManager = Mockito.mock(ImpressionsManager.class);
        TelemetrySyncTask telemetrySyncTask = Mockito.mock(TelemetrySyncTask.class);
        EventsTask eventsTask = Mockito.mock(EventsTask.class);
        SplitClientConfig config = Mockito.mock(SplitClientConfig.class);
        SplitTasks splitTasks = SplitTasks.build(null, null,
                impressionsManager, eventsTask, telemetrySyncTask);

        Mockito.when(_synchronizer.syncAll()).thenReturn(false).thenReturn(true);
        SyncManagerImp syncManager = new SyncManagerImp(splitTasks, false, _synchronizer, _pushManager, new LinkedBlockingQueue<>(),
                _gates, telemetryStorage, telemetrySynchronizer, config);
        syncManager.start();
        Thread.sleep(2000);
        Mockito.verify(_synchronizer, Mockito.times(1)).startPeriodicFetching();
        Mockito.verify(_synchronizer, Mockito.times(2)).syncAll();
        Mockito.verify(_pushManager, Mockito.times(0)).start();
        Mockito.verify(_gates, Mockito.times(1)).sdkInternalReady();
        Mockito.verify(telemetrySynchronizer, Mockito.times(1)).synchronizeConfig(Mockito.anyObject(), Mockito.anyLong(), Mockito.anyObject(), Mockito.anyObject());
    }
}
