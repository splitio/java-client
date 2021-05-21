package io.split.engine.common;

import io.split.client.SplitClientConfig;
import io.split.engine.SDKReadinessGates;
import io.split.telemetry.storage.InMemoryTelemetryStorage;
import io.split.telemetry.storage.TelemetryStorage;
import io.split.telemetry.synchronizer.TelemetrySynchronizer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.concurrent.LinkedBlockingQueue;

public class SyncManagerTest {
    private static final int BACKOFF_BASE = 1;
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
        TelemetryStorage telemetryStorage = new InMemoryTelemetryStorage();
        _gates.sdkInternalReady();
        TelemetrySynchronizer telemetrySynchronizer = Mockito.mock(TelemetrySynchronizer.class);
        SplitClientConfig config = Mockito.mock(SplitClientConfig.class);
        Mockito.when(_synchronizer.syncAll()).thenReturn(true);
        SyncManagerImp syncManager = new SyncManagerImp(false, _synchronizer, _pushManager, new LinkedBlockingQueue<>(), BACKOFF_BASE, _gates, telemetryStorage, telemetrySynchronizer, config);
        syncManager.start();
        Thread.sleep(1000);
        Mockito.verify(_synchronizer, Mockito.times(1)).startPeriodicFetching();
        Mockito.verify(_synchronizer, Mockito.times(1)).syncAll();
        Mockito.verify(_pushManager, Mockito.times(0)).start();
    }

    @Test
    public void startWithStreamingTrueShouldStartSyncAll() throws InterruptedException {
        TelemetryStorage telemetryStorage = new InMemoryTelemetryStorage();
        TelemetrySynchronizer telemetrySynchronizer = Mockito.mock(TelemetrySynchronizer.class);
        SplitClientConfig config = Mockito.mock(SplitClientConfig.class);
        Mockito.when(_synchronizer.syncAll()).thenReturn(true);
        SyncManager sm = new SyncManagerImp(true, _synchronizer, _pushManager, new LinkedBlockingQueue<>(), BACKOFF_BASE, _gates, telemetryStorage, telemetrySynchronizer, config);
        sm.start();
        Thread.sleep(1000);
        Mockito.verify(_synchronizer, Mockito.times(0)).startPeriodicFetching();
        Mockito.verify(_synchronizer, Mockito.times(1)).syncAll();
        Mockito.verify(_pushManager, Mockito.times(1)).start();
    }

    @Test
    public void onStreamingAvailable() throws InterruptedException {
        TelemetryStorage telemetryStorage = new InMemoryTelemetryStorage();
        LinkedBlockingQueue<PushManager.Status> messsages = new LinkedBlockingQueue<>();
        TelemetrySynchronizer telemetrySynchronizer = Mockito.mock(TelemetrySynchronizer.class);
        SplitClientConfig config = Mockito.mock(SplitClientConfig.class);

        SyncManagerImp syncManager = new SyncManagerImp(true, _synchronizer, _pushManager, messsages, BACKOFF_BASE, _gates, telemetryStorage, telemetrySynchronizer, config);
        Thread t = new Thread(syncManager::incomingPushStatusHandler);
        t.start();
        messsages.offer(PushManager.Status.STREAMING_READY);
        Thread.sleep(500);
        Mockito.verify(_synchronizer, Mockito.times(1)).stopPeriodicFetching();
        Mockito.verify(_synchronizer, Mockito.times(1)).syncAll();
        Mockito.verify(_pushManager, Mockito.times(1)).startWorkers();
        t.interrupt();
    }

    @Test
    public void onStreamingDisabled() throws InterruptedException {
        TelemetryStorage telemetryStorage = new InMemoryTelemetryStorage();
        LinkedBlockingQueue<PushManager.Status> messsages = new LinkedBlockingQueue<>();
        TelemetrySynchronizer telemetrySynchronizer = Mockito.mock(TelemetrySynchronizer.class);
        SplitClientConfig config = Mockito.mock(SplitClientConfig.class);
        SyncManagerImp syncManager = new SyncManagerImp(true, _synchronizer, _pushManager, messsages, BACKOFF_BASE, _gates, telemetryStorage, telemetrySynchronizer, config);
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
        SplitClientConfig config = Mockito.mock(SplitClientConfig.class);
        SyncManagerImp syncManager = new SyncManagerImp(true, _synchronizer, _pushManager, messsages, BACKOFF_BASE, _gates, telemetryStorage, telemetrySynchronizer, config);
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
        SplitClientConfig config = Mockito.mock(SplitClientConfig.class);
        SyncManagerImp syncManager = new SyncManagerImp(true, _synchronizer, _pushManager, messsages, BACKOFF_BASE, _gates, telemetryStorage, telemetrySynchronizer, config);
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
        SplitClientConfig config = Mockito.mock(SplitClientConfig.class);
        SyncManagerImp syncManager = new SyncManagerImp(true, _synchronizer, _pushManager, messsages, BACKOFF_BASE, _gates, telemetryStorage, telemetrySynchronizer, config);
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
        SplitClientConfig config = Mockito.mock(SplitClientConfig.class);
        Mockito.when(_synchronizer.syncAll()).thenReturn(true);
        SyncManagerImp syncManager = new SyncManagerImp(true, _synchronizer, _pushManager, messsages, BACKOFF_BASE, _gates, telemetryStorage, telemetrySynchronizer, config);
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
        SplitClientConfig config = Mockito.mock(SplitClientConfig.class);
        Mockito.when(_synchronizer.syncAll()).thenReturn(false).thenReturn(true);
        SyncManagerImp syncManager = new SyncManagerImp(false, _synchronizer, _pushManager, new LinkedBlockingQueue<>(), BACKOFF_BASE, _gates, telemetryStorage, telemetrySynchronizer, config);
        syncManager.start();
        Thread.sleep(2000);
        Mockito.verify(_synchronizer, Mockito.times(1)).startPeriodicFetching();
        Mockito.verify(_synchronizer, Mockito.times(2)).syncAll();
        Mockito.verify(_pushManager, Mockito.times(0)).start();
        Mockito.verify(_gates, Mockito.times(1)).sdkInternalReady();
        Mockito.verify(telemetrySynchronizer, Mockito.times(1)).synchronizeConfig(Mockito.anyObject(), Mockito.anyLong(), Mockito.anyObject(), Mockito.anyObject());
    }
}
