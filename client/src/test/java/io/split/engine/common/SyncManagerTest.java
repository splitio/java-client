package io.split.engine.common;

import io.split.engine.SDKReadinessGates;
import io.split.telemetry.storage.InMemoryTelemetryStorage;
import io.split.telemetry.storage.TelemetryStorage;
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
        SyncManagerImp syncManager = new SyncManagerImp(false, _synchronizer, _pushManager, new LinkedBlockingQueue<>(), BACKOFF_BASE, _gates, telemetryStorage);
        syncManager.start();
        Thread.sleep(1000);
        Mockito.verify(_synchronizer, Mockito.times(1)).startPeriodicFetching();
        Mockito.verify(_synchronizer, Mockito.times(1)).syncAll();
        Mockito.verify(_pushManager, Mockito.times(0)).start();
    }

    @Test
    public void startWithStreamingTrueShouldStartSyncAll() {
        TelemetryStorage telemetryStorage = new InMemoryTelemetryStorage();
        SyncManager sm = new SyncManagerImp(true, _synchronizer, _pushManager, new LinkedBlockingQueue<>(), BACKOFF_BASE, _gates, telemetryStorage);
        sm.start();
        Mockito.verify(_synchronizer, Mockito.times(0)).startPeriodicFetching();
        Mockito.verify(_synchronizer, Mockito.times(1)).syncAll();
        Mockito.verify(_pushManager, Mockito.times(1)).start();
    }

    @Test
    public void onStreamingAvailable() throws InterruptedException {
        TelemetryStorage telemetryStorage = new InMemoryTelemetryStorage();
        LinkedBlockingQueue<PushManager.Status> messsages = new LinkedBlockingQueue<>();
        SyncManagerImp syncManager = new SyncManagerImp(true, _synchronizer, _pushManager, messsages, BACKOFF_BASE, _gates, telemetryStorage);
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
        SyncManagerImp syncManager = new SyncManagerImp(true, _synchronizer, _pushManager, messsages, BACKOFF_BASE, _gates, telemetryStorage);
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
        SyncManagerImp syncManager = new SyncManagerImp(true, _synchronizer, _pushManager, messsages, BACKOFF_BASE, _gates, telemetryStorage);
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
        SyncManagerImp syncManager = new SyncManagerImp(true, _synchronizer, _pushManager, messsages, BACKOFF_BASE, _gates, telemetryStorage);
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
        SyncManagerImp syncManager = new SyncManagerImp(true, _synchronizer, _pushManager, messsages, BACKOFF_BASE, _gates, telemetryStorage);
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
        SyncManagerImp syncManager = new SyncManagerImp(true, _synchronizer, _pushManager, messsages, BACKOFF_BASE, _gates, telemetryStorage);
        syncManager.start();
        messsages.offer(PushManager.Status.STREAMING_BACKOFF);
        Thread.sleep(1200);
        Mockito.verify(_synchronizer, Mockito.times(1)).startPeriodicFetching();
        Mockito.verify(_synchronizer, Mockito.times(1)).syncAll();
        Mockito.verify(_pushManager, Mockito.times(2)).start();
    }
}
