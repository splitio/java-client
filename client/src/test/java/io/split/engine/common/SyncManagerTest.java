package io.split.engine.common;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.concurrent.LinkedBlockingQueue;

public class SyncManagerTest {
    private static final int BACKOFF_BASE = 1;
    private Synchronizer _synchronizer;
    private PushManager _pushManager;

    @Before
    public void setUp() {
        _synchronizer = Mockito.mock(Synchronizer.class);
        _pushManager = Mockito.mock(PushManager.class);
    }

    @Test
    public void startWithStreamingFalseShouldStartPolling() {
        SyncManagerImp syncManager = new SyncManagerImp(false, _synchronizer, _pushManager, new LinkedBlockingQueue<>(), BACKOFF_BASE);
        syncManager.start();
        Mockito.verify(_synchronizer, Mockito.times(1)).startPeriodicFetching();
        Mockito.verify(_synchronizer, Mockito.times(1)).syncAll();
        Mockito.verify(_pushManager, Mockito.times(0)).start();
    }

    @Test
    public void startWithStreamingTrueShouldStartSyncAll() {
        SyncManager sm = new SyncManagerImp(true, _synchronizer, _pushManager, new LinkedBlockingQueue<>(), BACKOFF_BASE);
        sm.start();
        Mockito.verify(_synchronizer, Mockito.times(0)).startPeriodicFetching();
        Mockito.verify(_synchronizer, Mockito.times(1)).syncAll();
        Mockito.verify(_pushManager, Mockito.times(1)).start();
    }

    @Test
    public void onStreamingAvailable() throws InterruptedException {
        LinkedBlockingQueue<PushManager.Status> messsages = new LinkedBlockingQueue<>();
        SyncManagerImp syncManager = new SyncManagerImp(true, _synchronizer, _pushManager, messsages, BACKOFF_BASE);
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
        LinkedBlockingQueue<PushManager.Status> messsages = new LinkedBlockingQueue<>();
        SyncManagerImp syncManager = new SyncManagerImp(true, _synchronizer, _pushManager, messsages, BACKOFF_BASE);
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
        LinkedBlockingQueue<PushManager.Status> messsages = new LinkedBlockingQueue<>();
        SyncManagerImp syncManager = new SyncManagerImp(true, _synchronizer, _pushManager, messsages, BACKOFF_BASE);
        Thread t = new Thread(syncManager::incomingPushStatusHandler);
        t.start();
        messsages.offer(PushManager.Status.STREAMING_OFF);
        Thread.sleep(500);
        Mockito.verify(_pushManager, Mockito.times(1)).stop();
        t.interrupt();
    }

    @Test
    public void onConnected() throws InterruptedException {
        LinkedBlockingQueue<PushManager.Status> messsages = new LinkedBlockingQueue<>();
        SyncManagerImp syncManager = new SyncManagerImp(true, _synchronizer, _pushManager, messsages, BACKOFF_BASE);
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
        LinkedBlockingQueue<PushManager.Status> messsages = new LinkedBlockingQueue<>();
        SyncManagerImp syncManager = new SyncManagerImp(true, _synchronizer, _pushManager, messsages, BACKOFF_BASE);
        Thread t = new Thread(syncManager::incomingPushStatusHandler);
        t.start();
        messsages.offer(PushManager.Status.STREAMING_OFF);
        Thread.sleep(500);
        Mockito.verify(_synchronizer, Mockito.times(1)).startPeriodicFetching();
        t.interrupt();
    }

    @Test
    public void onDisconnectAndReconnect() throws InterruptedException { // Check with mauro. reconnect should call pushManager.start again, right?
        LinkedBlockingQueue<PushManager.Status> messsages = new LinkedBlockingQueue<>();
        SyncManagerImp syncManager = new SyncManagerImp(true, _synchronizer, _pushManager, messsages, BACKOFF_BASE);
        syncManager.start();
        messsages.offer(PushManager.Status.STREAMING_BACKOFF);
        Thread.sleep(1200);
        Mockito.verify(_synchronizer, Mockito.times(1)).startPeriodicFetching();
        Mockito.verify(_synchronizer, Mockito.times(1)).syncAll();
        Mockito.verify(_pushManager, Mockito.times(2)).start();
    }
}
