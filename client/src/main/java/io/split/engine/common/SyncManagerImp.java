package io.split.engine.common;

import com.google.common.annotations.VisibleForTesting;
import io.split.engine.experiments.RefreshableSplitFetcherProvider;
import io.split.engine.segments.RefreshableSegmentFetcher;
import io.split.engine.sse.SSEHandler;
import io.split.engine.sse.SSEHandlerImp;
import io.split.engine.sse.dtos.ErrorNotification;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.common.base.Preconditions.checkNotNull;

public class SyncManagerImp implements SyncManager {
    private static final Logger _log = LoggerFactory.getLogger(SyncManager.class);

    private final AtomicBoolean _streamingEnabledConfig;
    private final Synchronizer _synchronizer;
    private final PushManager _pushManager;
    private final SSEHandler _sseHandler;
    private final AtomicBoolean _shutdown;

    @VisibleForTesting
    /* package private */ SyncManagerImp(boolean streamingEnabledConfig,
                                         Synchronizer synchronizer,
                                         PushManager pushManager,
                                         SSEHandler sseHandler) {
        _streamingEnabledConfig = new AtomicBoolean(streamingEnabledConfig);
        _synchronizer = checkNotNull(synchronizer);
        _pushManager = checkNotNull(pushManager);
        _sseHandler = checkNotNull(sseHandler);
        _shutdown = new AtomicBoolean(false);

        _sseHandler.registerFeedbackListener(this);
    }

    public static SyncManagerImp build(boolean streamingEnabledConfig,
                                        RefreshableSplitFetcherProvider refreshableSplitFetcherProvider,
                                        RefreshableSegmentFetcher segmentFetcher,
                                        String authUrl,
                                        CloseableHttpClient httpClient,
                                        String streamingServiceUrl,
                                        int authRetryBackOffBase) {
        SSEHandler sseHandler = SSEHandlerImp.build(streamingServiceUrl, refreshableSplitFetcherProvider, segmentFetcher);

        return new SyncManagerImp(streamingEnabledConfig,
                new SynchronizerImp(refreshableSplitFetcherProvider, segmentFetcher),
                PushManagerImp.build(authUrl, httpClient, sseHandler, authRetryBackOffBase),
                sseHandler);
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
        _shutdown.set(true);
        _synchronizer.stopPeriodicFetching();
        _pushManager.stop();
    }

    @Override
    public void onStreamingAvailable() {
        _synchronizer.stopPeriodicFetching();
        _sseHandler.startWorkers();
        _synchronizer.syncAll();
    }

    @Override
    public void onStreamingDisabled() {
        _sseHandler.stopWorkers();
        _synchronizer.startPeriodicFetching();
    }

    @Override
    public void onStreamingShutdown() {
        _pushManager.stop();
    }

    @Override
    public void onErrorNotification(ErrorNotification errorNotification) {
        _pushManager.stop();
        startStreamingMode();
    }

    @Override
    public void onConnected() {
        _log.debug("Event source client connected ...");
        _synchronizer.stopPeriodicFetching();
        _synchronizer.syncAll();
        _sseHandler.startWorkers();
    }

    @Override
    public void onDisconnect(Boolean reconnect) {
        _log.debug("Event source client disconnected ...");

        if (_shutdown.get()) {
            return;
        }

        if (reconnect) {
            startStreamingMode();
            return;
        }

        _synchronizer.startPeriodicFetching();
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
