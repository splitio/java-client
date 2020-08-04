package io.split.engine.common;

import com.google.common.annotations.VisibleForTesting;
import io.split.engine.experiments.RefreshableSplitFetcherProvider;
import io.split.engine.segments.RefreshableSegmentFetcher;
import io.split.engine.sse.NotificationManagerKeeper;
import io.split.engine.sse.NotificationManagerKeeperImp;
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
    private final AtomicBoolean _shutdown;

    @VisibleForTesting
    /* package private */ SyncManagerImp(boolean streamingEnabledConfig,
                                         Synchronizer synchronizer,
                                         PushManager pushManager,
                                         SSEHandler sseHandler,
                                         NotificationManagerKeeper notificationManagerKeeper) {
        _streamingEnabledConfig = new AtomicBoolean(streamingEnabledConfig);
        _synchronizer = checkNotNull(synchronizer);
        _pushManager = checkNotNull(pushManager);
        _shutdown = new AtomicBoolean(false);

        sseHandler.registerFeedbackListener(this);
        notificationManagerKeeper.registerNotificationKeeperListener(this);
    }

    public static SyncManagerImp build(boolean streamingEnabledConfig,
                                        RefreshableSplitFetcherProvider refreshableSplitFetcherProvider,
                                        RefreshableSegmentFetcher segmentFetcher,
                                        String authUrl,
                                        CloseableHttpClient httpClient,
                                        String streamingServiceUrl,
                                        int authRetryBackOffBase) {
        NotificationManagerKeeper notificationManagerKeeper = new NotificationManagerKeeperImp();
        SSEHandler sseHandler = SSEHandlerImp.build(streamingServiceUrl, refreshableSplitFetcherProvider, segmentFetcher, notificationManagerKeeper);

        return new SyncManagerImp(streamingEnabledConfig,
                new SynchronizerImp(refreshableSplitFetcherProvider, segmentFetcher),
                PushManagerImp.build(authUrl, httpClient, sseHandler, authRetryBackOffBase),
                sseHandler,
                notificationManagerKeeper);
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
        _pushManager.startWorkers();
        _synchronizer.syncAll();
    }

    @Override
    public void onStreamingDisabled() {
        _pushManager.stopWorkers();
        _synchronizer.startPeriodicFetching();
    }

    @Override
    public void onStreamingShutdown() {
        _pushManager.stop();
    }

    @Override
    public void onErrorNotification(ErrorNotification errorNotification) {
        if (errorNotification.getCode() >= 40140 && errorNotification.getCode() <= 40149) {
            _pushManager.stop();
            startStreamingMode();
            return;
        }

        if (errorNotification.getCode() >= 40000 && errorNotification.getCode() <= 49999) {
            _pushManager.stop();
            startPollingMode();
            return;
        }
    }

    @Override
    public void onConnected() {
        _log.debug("Event source client connected ...");
        _synchronizer.stopPeriodicFetching();
        _synchronizer.syncAll();
        _pushManager.startWorkers();
    }

    @Override
    public void onDisconnect(Boolean reconnect) {
        _log.debug(String.format("Event source client disconnected. Reconnect: %s.", reconnect));

        if (_shutdown.get()) {
            return;
        }

        _synchronizer.startPeriodicFetching();

        if (reconnect) {
            startStreamingMode();
        }
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
