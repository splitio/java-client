package io.split.engine.common;

import io.split.engine.sse.SSEHandler;
import org.junit.Test;
import org.mockito.Mockito;

public class SyncManagerTest {
    private final Synchronizer _synchronizer;
    private final PushManager _pushManager;
    private final SSEHandler _sseHandler;

    public SyncManagerTest() {
        _synchronizer = Mockito.mock(Synchronizer.class);
        _pushManager = Mockito.mock(PushManager.class);
        _sseHandler = Mockito.mock(SSEHandler.class);
    }

    @Test
    public void startWithStreamingFalseShouldStartPolling() {
        SyncManager syncManager = new SyncManagerImp(false, _synchronizer, _pushManager, _sseHandler);
        syncManager.start();

        Mockito.verify(_synchronizer, Mockito.times(1)).startPeriodicFetching();
        Mockito.verify(_synchronizer, Mockito.times(0)).syncAll();
        Mockito.verify(_pushManager, Mockito.times(0)).start();
    }

    @Test
    public void startWithStreamingTrueShouldStartPolling() {
        SyncManager syncManager = new SyncManagerImp(true, _synchronizer, _pushManager, _sseHandler);
        syncManager.start();

        Mockito.verify(_synchronizer, Mockito.times(0)).startPeriodicFetching();
        Mockito.verify(_synchronizer, Mockito.times(1)).syncAll();
        Mockito.verify(_pushManager, Mockito.times(1)).start();
    }

    @Test
    public void onStreamingAvailable() {
        SyncManagerImp syncManager = new SyncManagerImp(true, _synchronizer, _pushManager, _sseHandler);

        syncManager.onStreamingAvailable();

        Mockito.verify(_synchronizer, Mockito.times(1)).stopPeriodicFetching();
        Mockito.verify(_synchronizer, Mockito.times(1)).syncAll();
        Mockito.verify(_sseHandler, Mockito.times(1)).startWorkers();
    }

    @Test
    public void onStreamingDisabled() {
        SyncManagerImp syncManager = new SyncManagerImp(true, _synchronizer, _pushManager, _sseHandler);

        syncManager.onStreamingDisabled();

        Mockito.verify(_synchronizer, Mockito.times(1)).startPeriodicFetching();
        Mockito.verify(_sseHandler, Mockito.times(1)).stop();
    }

    @Test
    public void onStreamingShutdown() {
        SyncManagerImp syncManager = new SyncManagerImp(true, _synchronizer, _pushManager, _sseHandler);

        syncManager.onStreamingShutdown();

        Mockito.verify(_pushManager, Mockito.times(1)).stop();
    }
}
