package io.split.client.impressions;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.split.client.SplitClientConfig;
import io.split.client.dtos.KeyImpression;
import io.split.client.dtos.TestImpressions;
import io.split.telemetry.domain.enums.ImpressionsDataTypeEnum;
import io.split.telemetry.storage.TelemetryRuntimeProducer;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by patricioe on 6/17/16.
 */
public class ImpressionsManagerImpl implements ImpressionsManager, Closeable {

    private static final Logger _log = LoggerFactory.getLogger(ImpressionsManagerImpl.class);

    private static final long BULK_INITIAL_DELAY_SECONDS = 10L;
    private static final long COUNT_INITIAL_DELAY_SECONDS = 100L;
    private static final long COUNT_REFRESH_RATE_SECONDS = 30 * 60;
    private static final long LAST_SEEN_CACHE_SIZE = 500000; // cache up to 500k impression hashes

    private final SplitClientConfig _config;
    private final ImpressionsStorage _storage;
    private final ScheduledExecutorService _scheduler;
    private final ImpressionsSender _impressionsSender;
    private final ImpressionObserver _impressionObserver;
    private final ImpressionCounter _counter;
    private final ImpressionListener _listener;
    private final ImpressionsManager.Mode _mode;
    private final TelemetryRuntimeProducer _telemetryRuntimeProducer;

    public static ImpressionsManagerImpl instance(CloseableHttpClient client,
                                                  SplitClientConfig config,
                                                  List<ImpressionListener> listeners,
                                                  TelemetryRuntimeProducer telemetryRuntimeProducer) throws URISyntaxException {
        return new ImpressionsManagerImpl(client, config, null, listeners, telemetryRuntimeProducer);
    }

    public static ImpressionsManagerImpl instanceForTest(CloseableHttpClient client,
                                                         SplitClientConfig config,
                                                         ImpressionsSender impressionsSender,
                                                         List<ImpressionListener> listeners,
                                                         TelemetryRuntimeProducer telemetryRuntimeProducer) throws URISyntaxException {
        return new ImpressionsManagerImpl(client, config, impressionsSender, listeners, telemetryRuntimeProducer);
    }

    private ImpressionsManagerImpl(CloseableHttpClient client,
                                   SplitClientConfig config,
                                   ImpressionsSender impressionsSender,
                                   List<ImpressionListener> listeners,
                                   TelemetryRuntimeProducer telemetryRuntimeProducer) throws URISyntaxException {


        _config = checkNotNull(config);
        _mode = checkNotNull(config.impressionsMode());
        _telemetryRuntimeProducer = checkNotNull(telemetryRuntimeProducer);
        _storage = new InMemoryImpressionsStorage(config.impressionsQueueSize());
        _impressionObserver = new ImpressionObserver(LAST_SEEN_CACHE_SIZE);
        _counter = new ImpressionCounter();
        _impressionsSender = (null != impressionsSender) ? impressionsSender
                : HttpImpressionsSender.create(client, URI.create(config.eventsEndpoint()), _mode, telemetryRuntimeProducer);

        _scheduler = buildExecutor();
        _scheduler.scheduleAtFixedRate(this::sendImpressions, BULK_INITIAL_DELAY_SECONDS, config.impressionsRefreshRate(), TimeUnit.SECONDS);
        if (Mode.OPTIMIZED.equals(_mode)) {
            _scheduler.scheduleAtFixedRate(this::sendImpressionCounters, COUNT_INITIAL_DELAY_SECONDS, COUNT_REFRESH_RATE_SECONDS, TimeUnit.SECONDS);
        }

        _listener = (null != listeners && !listeners.isEmpty()) ? new ImpressionListener.FederatedImpressionListener(listeners)
                : new ImpressionListener.NoopImpressionListener();
    }

    private static boolean shouldQueueImpression(Impression i) {
        return Objects.isNull(i.pt()) ||
                ImpressionUtils.truncateTimeframe(i.pt()) != ImpressionUtils.truncateTimeframe(i.time());
    }

    @Override
    public void track(Impression impression) {
        if (null == impression) {
            return;
        }

        impression = impression.withPreviousTime(_impressionObserver.testAndSet(impression));
        _listener.log(impression);

        if (Mode.OPTIMIZED.equals(_mode)) {
            _counter.inc(impression.split(), impression.time(), 1);
        }

        if (Mode.DEBUG.equals(_mode) || shouldQueueImpression(impression)) {
            if (shouldQueueImpression(impression)) {
                _telemetryRuntimeProducer.recordImpressionStats(ImpressionsDataTypeEnum.IMPRESSIONS_DEDUPED, 1);
            }
            if (_storage.put(KeyImpression.fromImpression(impression))) {
                _telemetryRuntimeProducer.recordImpressionStats(ImpressionsDataTypeEnum.IMPRESSIONS_QUEUED, 1);
            }
            else {
                _telemetryRuntimeProducer.recordImpressionStats(ImpressionsDataTypeEnum.IMPRESSIONS_DROPPED, 1);            }
        }
    }

    @Override
    public void close() {
        try {
            _listener.close();
            _log.info("Successful shutdown of ImpressionListener");
            _scheduler.shutdown();
            sendImpressions();
            _scheduler.awaitTermination(_config.waitBeforeShutdown(), TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            _log.warn("Unable to close ImpressionsManager properly", e);
        }

    }

    @VisibleForTesting
        /* package private */ void sendImpressions() {
        if (_storage.isFull()) {
            _log.warn("Split SDK impressions queue is full. Impressions may have been dropped. Consider increasing capacity.");
        }

        long start = System.currentTimeMillis();
        List<KeyImpression> impressions = _storage.pop();
        if (impressions.isEmpty()) {
            return; // Nothing to send
        }

        _impressionsSender.postImpressionsBulk(TestImpressions.fromKeyImpressions(impressions));
        if (_config.debugEnabled()) {
            _log.info(String.format("Posting %d Split impressions took %d millis",
                    impressions.size(), (System.currentTimeMillis() - start)));
        }
    }

    @VisibleForTesting
        /* package private */ void sendImpressionCounters() {
        if (!_counter.isEmpty()) {
            _impressionsSender.postCounters(_counter.popAll());
        }
    }

    private ScheduledExecutorService buildExecutor() {
        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat("Split-ImpressionsManager-%d")
                .build();
        return Executors.newScheduledThreadPool(2, threadFactory);
    }
}
