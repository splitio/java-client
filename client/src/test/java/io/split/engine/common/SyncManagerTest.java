package io.split.engine.common;

import io.split.engine.sse.PushStatusTracker;
import io.split.engine.sse.SSEHandler;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.concurrent.LinkedBlockingQueue;

public class SyncManagerTest {
    private final Synchronizer _synchronizer;
    private final PushManager _pushManager;
    private final SSEHandler _sseHandler;
    private final PushStatusTracker _pushStatusTracker;

    public SyncManagerTest() {
        _synchronizer = Mockito.mock(Synchronizer.class);
        _pushManager = Mockito.mock(PushManager.class);
        _sseHandler = Mockito.mock(SSEHandler.class);
        _pushStatusTracker = Mockito.mock(PushStatusTracker.class);
    }
        
    @Test
    public void startWithStreamingFalseShouldStartPolling() {
        SyncManagerImp syncManager = new SyncManagerImp(false, _synchronizer, _pushManager, new LinkedBlockingQueue<>());
        syncManager.start();
        Mockito.verify(_synchronizer, Mockito.times(1)).startPeriodicFetching();
        Mockito.verify(_synchronizer, Mockito.times(0)).syncAll();
        Mockito.verify(_pushManager, Mockito.times(0)).start();
    }

    @Test
    public void startWithStreamingTrueShouldStartSyncAll() {
        SyncManager sm = new SyncManagerImp(true, _synchronizer, _pushManager, new LinkedBlockingQueue<>());
        sm.start();
        Mockito.verify(_synchronizer, Mockito.times(0)).startPeriodicFetching();
        Mockito.verify(_synchronizer, Mockito.times(1)).syncAll();
        Mockito.verify(_pushManager, Mockito.times(1)).start();
    }

    @Test
    public void onStreamingAvailable() throws InterruptedException {
        LinkedBlockingQueue<PushManager.Status> messsages = new LinkedBlockingQueue<>();
        SyncManagerImp syncManager = new SyncManagerImp(true, _synchronizer, _pushManager, messsages);
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
        SyncManagerImp syncManager = new SyncManagerImp(true, _synchronizer, _pushManager, messsages);
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
        SyncManagerImp syncManager = new SyncManagerImp(true, _synchronizer, _pushManager, messsages);
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
        SyncManagerImp syncManager = new SyncManagerImp(true, _synchronizer, _pushManager, messsages);
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
        SyncManagerImp syncManager = new SyncManagerImp(true, _synchronizer, _pushManager, messsages);
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
        SyncManagerImp syncManager = new SyncManagerImp(true, _synchronizer, _pushManager, messsages);
        syncManager.start();
        messsages.offer(PushManager.Status.STREAMING_BACKOFF);
        Thread.sleep(500);
        Mockito.verify(_synchronizer, Mockito.times(1)).startPeriodicFetching();
        Mockito.verify(_synchronizer, Mockito.times(1)).syncAll();
        Mockito.verify(_pushManager, Mockito.times(2)).start();
    }
}
