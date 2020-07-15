package io.split.engine.common;

import io.split.engine.sse.SSEHandler;
import io.split.engine.sse.listeners.NotificationKeeperListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

public class SyncManagerImp implements SyncManager, NotificationKeeperListener {
    private static final Logger _log = LoggerFactory.getLogger(SyncManager.class);

    private final AtomicBoolean _streamingEnabledConfig;
    private final Synchronizer _synchronizer;
    private final PushManager _pushManager;
    private final SSEHandler _sseHandler;

    public SyncManagerImp(boolean streamingEnabledConfig,
                          Synchronizer synchronizer,
                          PushManager pushManager,
                          SSEHandler sseHandler) {
        _streamingEnabledConfig = new AtomicBoolean(streamingEnabledConfig);
        _synchronizer = synchronizer;
        _pushManager = pushManager;
        _sseHandler = sseHandler;
    }

    @Override
    public void start() {
        if (_streamingEnabledConfig.get()) {
            startStreamingMode();
        } else {
            startPollingMode();
        }
    }

    @Override
    public void shutdown() {
        _synchronizer.stopPeriodicFetching();
        _pushManager.stop();
    }

    @Override
    public void onStreamingAvailable() {
        _synchronizer.stopPeriodicFetching();
        _synchronizer.syncAll();
        _sseHandler.startWorkers();
    }

    @Override
    public void onStreamingDisabled() {
        _sseHandler.stop();
        _synchronizer.startPeriodicFetching();
    }

    @Override
    public void onStreamingShutdown() {
        _pushManager.stop();
    }

    private void startStreamingMode() {
        _log.debug("Starting in streaming mode ...");
        _synchronizer.syncAll();
        _pushManager.start();
    }

    private void startPollingMode() {
        _log.debug("Starting in polling mode ...");
        _synchronizer.startPeriodicFetching();
    }
}
