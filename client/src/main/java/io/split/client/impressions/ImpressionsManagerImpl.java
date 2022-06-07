package io.split.client.impressions;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.split.client.SplitClientConfig;
import io.split.client.dtos.KeyImpression;
import io.split.client.dtos.TestImpressions;
import io.split.client.impressions.strategy.ProcessImpressionFactory;
import io.split.client.impressions.strategy.ProcessImpressionStrategy;
import io.split.storages.enums.OperationMode;
import io.split.telemetry.domain.enums.ImpressionsDataTypeEnum;
import io.split.telemetry.storage.TelemetryRuntimeProducer;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
    private final ImpressionsStorageProducer _impressionsStorageProducer;
    private final ImpressionsStorageConsumer _impressionsStorageConsumer;
    private final ScheduledExecutorService _scheduler;
    private final ImpressionsSender _impressionsSender;
    private final ImpressionObserver _impressionObserver;
    private final ImpressionCounter _counter;
    private final ImpressionListener _listener;
    private final ImpressionsManager.Mode _mode;
    private final TelemetryRuntimeProducer _telemetryRuntimeProducer;
    private final boolean _addPreviousTimeEnabled;
    private final boolean _isOptimized;
    private final OperationMode _operationMode;
    private final ProcessImpressionStrategy _processImpressionStrategy;
    private final UniqueKeysTracker _uniqueKeysTracker;

    public static ImpressionsManagerImpl instance(CloseableHttpClient client,
                                                  SplitClientConfig config,
                                                  List<ImpressionListener> listeners,
                                                  TelemetryRuntimeProducer telemetryRuntimeProducer,
                                                  ImpressionsStorageConsumer impressionsStorageConsumer,
                                                  ImpressionsStorageProducer impressionsStorageProducer,
                                                  UniqueKeysTracker uniqueKeysTracker) throws URISyntaxException {
        return new ImpressionsManagerImpl(client, config, null, listeners, telemetryRuntimeProducer, impressionsStorageConsumer, impressionsStorageProducer, uniqueKeysTracker);
    }

    public static ImpressionsManagerImpl instanceForTest(CloseableHttpClient client,
                                                         SplitClientConfig config,
                                                         ImpressionsSender impressionsSender,
                                                         List<ImpressionListener> listeners,
                                                         TelemetryRuntimeProducer telemetryRuntimeProducer,
                                                         ImpressionsStorageConsumer impressionsStorageConsumer,
                                                         ImpressionsStorageProducer impressionsStorageProducer,
                                                         UniqueKeysTracker uniqueKeysTracker) throws URISyntaxException {
        return new ImpressionsManagerImpl(client, config, impressionsSender, listeners, telemetryRuntimeProducer, impressionsStorageConsumer, impressionsStorageProducer, uniqueKeysTracker);
    }

    private ImpressionsManagerImpl(CloseableHttpClient client,
                                   SplitClientConfig config,
                                   ImpressionsSender impressionsSender,
                                   List<ImpressionListener> listeners,
                                   TelemetryRuntimeProducer telemetryRuntimeProducer,
                                   ImpressionsStorageConsumer impressionsStorageConsumer,
                                   ImpressionsStorageProducer impressionsStorageProducer,
                                   UniqueKeysTracker uniqueKeysTracker) throws URISyntaxException {


        _config = checkNotNull(config);
        _mode = checkNotNull(config.impressionsMode());
        _telemetryRuntimeProducer = checkNotNull(telemetryRuntimeProducer);
        _impressionsStorageConsumer = checkNotNull(impressionsStorageConsumer);
        _impressionsStorageProducer = checkNotNull(impressionsStorageProducer);
        _impressionObserver = new ImpressionObserver(LAST_SEEN_CACHE_SIZE);
        _impressionsSender = (null != impressionsSender) ? impressionsSender
                : HttpImpressionsSender.create(client, URI.create(config.eventsEndpoint()), _mode, telemetryRuntimeProducer);

        _scheduler = buildExecutor();
        _scheduler.scheduleAtFixedRate(this::sendImpressions, BULK_INITIAL_DELAY_SECONDS, config.impressionsRefreshRate(), TimeUnit.SECONDS);

        _listener = (null != listeners && !listeners.isEmpty()) ? new ImpressionListener.FederatedImpressionListener(listeners)
                : new ImpressionListener.NoopImpressionListener();

        _operationMode = config.operationMode();
        _addPreviousTimeEnabled = shouldAddPreviousTime();
        _counter = _addPreviousTimeEnabled ? new ImpressionCounter() : null;
        _isOptimized = _counter != null && shouldBeOptimized();
        _uniqueKeysTracker = uniqueKeysTracker;
        if (_isOptimized) {
            _scheduler.scheduleAtFixedRate(this::sendImpressionCounters, COUNT_INITIAL_DELAY_SECONDS, COUNT_REFRESH_RATE_SECONDS, TimeUnit.SECONDS);
        }
        ProcessImpressionFactory processImpressionFactory =  new ProcessImpressionFactory();
        _processImpressionStrategy = processImpressionFactory.createProcessImpression(config.impressionsMode());

    }

    private boolean shouldQueueImpression(Impression i) {
        return Objects.isNull(i.pt()) ||
                ImpressionUtils.truncateTimeframe(i.pt()) != ImpressionUtils.truncateTimeframe(i.time());
    }

    @Override
    public void track(List<Impression> impressions) {
        if (null == impressions) {
            return;
        }
        int totalImpressions = impressions.size();

        //impressions = processImpressions(impressions);
        impressions = _processImpressionStrategy.processImpressions(impressions, _impressionObserver, _counter, _addPreviousTimeEnabled, _uniqueKeysTracker);

        if (totalImpressions > impressions.size()) {
            _telemetryRuntimeProducer.recordImpressionStats(ImpressionsDataTypeEnum.IMPRESSIONS_DEDUPED, totalImpressions-impressions.size());
            totalImpressions = impressions.size();
        }
        long queued = _impressionsStorageProducer.put(impressions.stream().map(KeyImpression::fromImpression).collect(Collectors.toList()));
        if (queued < totalImpressions) {
            _telemetryRuntimeProducer.recordImpressionStats(ImpressionsDataTypeEnum.IMPRESSIONS_DROPPED, totalImpressions-queued);
        }
        _telemetryRuntimeProducer.recordImpressionStats(ImpressionsDataTypeEnum.IMPRESSIONS_QUEUED, queued);

        impressions.forEach(imp -> _listener.log(imp));
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
        if (_impressionsStorageConsumer.isFull()) {
            _log.warn("Split SDK impressions queue is full. Impressions may have been dropped. Consider increasing capacity.");
        }

        long start = System.currentTimeMillis();
        List<KeyImpression> impressions = _impressionsStorageConsumer.pop();
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



    private boolean shouldAddPreviousTime() {
            switch (_operationMode) {
                case STANDALONE:
                    return true;
                default:
                    return false;
            }
    }

    private boolean shouldBeOptimized() {
        if(!_addPreviousTimeEnabled)
            return false;
        switch (_mode) {
            case OPTIMIZED:
                return true;
            default:
                return false;
        }
    }

    @VisibleForTesting
    /* package private */ ImpressionCounter getCounter() {
        return _counter;
    }

    /**
     * Filter in case of deduping and format impressions to let them ready to be sent.
     * @param impressions
     * @return
     */
    private List<Impression> processImpressions(List<Impression> impressions) {
        if(!_addPreviousTimeEnabled) { //Only STANDALONE Mode needs to iterate over impressions to add previous time.
            return impressions;
        }

        List<Impression> impressionsToQueue = new ArrayList<>();
        for(Impression impression : impressions) {
            impression = impression.withPreviousTime(_impressionObserver.testAndSet(impression));
            if (_isOptimized) {
                _counter.inc(impression.split(), impression.time(), 1);
                if(!shouldQueueImpression(impression)) {
                    continue;
                }
            }
            impressionsToQueue.add(impression);
        }
        return impressionsToQueue;
    }
}
