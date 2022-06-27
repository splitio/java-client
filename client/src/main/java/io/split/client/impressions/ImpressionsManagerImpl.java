package io.split.client.impressions;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.split.client.SplitClientConfig;
import io.split.client.dtos.KeyImpression;
import io.split.client.dtos.TestImpressions;
import io.split.client.impressions.strategy.ProcessImpressionDebug;
import io.split.client.impressions.strategy.ProcessImpressionNone;
import io.split.client.impressions.strategy.ProcessImpressionOptimized;
import io.split.client.impressions.strategy.ProcessImpressionStrategy;
import io.split.storages.enums.OperationMode;
import io.split.telemetry.domain.enums.ImpressionsDataTypeEnum;
import io.split.telemetry.storage.TelemetryRuntimeProducer;
import io.split.telemetry.synchronizer.TelemetrySynchronizer;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.net.URISyntaxException;
import java.util.List;
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
    private final ImpressionListener _listener;
    private final ImpressionsManager.Mode _impressionsMode;
    private final OperationMode _operationMode;
    private TelemetryRuntimeProducer _telemetryRuntimeProducer;
    private ImpressionObserver impressionObserver;
    private ImpressionCounter counter;
    private ProcessImpressionStrategy processImpressionStrategy;
    private UniqueKeysTracker uniqueKeysTracker;

    public static ImpressionsManagerImpl instance(CloseableHttpClient client,
                                                  SplitClientConfig config,
                                                  List<ImpressionListener> listeners,
                                                  TelemetryRuntimeProducer telemetryRuntimeProducer,
                                                  ImpressionsStorageConsumer impressionsStorageConsumer,
                                                  ImpressionsStorageProducer impressionsStorageProducer,
                                                  ImpressionsSender impressionsSender,
                                                  TelemetrySynchronizer telemetrySynchronizer) throws URISyntaxException {
        return new ImpressionsManagerImpl(config, impressionsSender, listeners, telemetryRuntimeProducer, impressionsStorageConsumer, impressionsStorageProducer, telemetrySynchronizer);
    }

    public static ImpressionsManagerImpl instanceForTest(SplitClientConfig config,
                                                         ImpressionsSender impressionsSender,
                                                         List<ImpressionListener> listeners,
                                                         TelemetryRuntimeProducer telemetryRuntimeProducer,
                                                         ImpressionsStorageConsumer impressionsStorageConsumer,
                                                         ImpressionsStorageProducer impressionsStorageProducer,
                                                         TelemetrySynchronizer telemetrySynchronizer) throws URISyntaxException {
        return new ImpressionsManagerImpl(config, impressionsSender, listeners, telemetryRuntimeProducer, impressionsStorageConsumer, impressionsStorageProducer, telemetrySynchronizer);
    }

    private ImpressionsManagerImpl(SplitClientConfig config,
                                   ImpressionsSender impressionsSender,
                                   List<ImpressionListener> listeners,
                                   TelemetryRuntimeProducer telemetryRuntimeProducer,
                                   ImpressionsStorageConsumer impressionsStorageConsumer,
                                   ImpressionsStorageProducer impressionsStorageProducer,
                                   TelemetrySynchronizer telemetrySynchronizer) throws URISyntaxException {


        _config = checkNotNull(config);
        _impressionsMode = checkNotNull(config.impressionsMode());
        _impressionsStorageConsumer = checkNotNull(impressionsStorageConsumer);
        _impressionsStorageProducer = checkNotNull(impressionsStorageProducer);
        _telemetryRuntimeProducer = checkNotNull(telemetryRuntimeProducer);
        _impressionsSender = impressionsSender;

        _scheduler = buildExecutor();

        _listener = (null != listeners && !listeners.isEmpty()) ? new ImpressionListener.FederatedImpressionListener(listeners)
                : null;

        _operationMode = config.operationMode();
        switch (_impressionsMode){
            case OPTIMIZED:
                _scheduler.scheduleAtFixedRate(this::sendImpressionCounters, COUNT_INITIAL_DELAY_SECONDS, COUNT_REFRESH_RATE_SECONDS, TimeUnit.SECONDS);
                _scheduler.scheduleAtFixedRate(this::sendImpressions, BULK_INITIAL_DELAY_SECONDS, config.impressionsRefreshRate(), TimeUnit.SECONDS);
                counter = new ImpressionCounter();
                impressionObserver = new ImpressionObserver(LAST_SEEN_CACHE_SIZE);
                processImpressionStrategy = new ProcessImpressionOptimized(_listener!=null, impressionObserver, counter, _telemetryRuntimeProducer);
                break;
            case DEBUG:
                _scheduler.scheduleAtFixedRate(this::sendImpressions, BULK_INITIAL_DELAY_SECONDS, config.impressionsRefreshRate(), TimeUnit.SECONDS);
                impressionObserver = new ImpressionObserver(LAST_SEEN_CACHE_SIZE);
                processImpressionStrategy = new ProcessImpressionDebug(_listener!=null, impressionObserver);
                break;
            case NONE:
                _scheduler.scheduleAtFixedRate(this::sendImpressionCounters, COUNT_INITIAL_DELAY_SECONDS, COUNT_REFRESH_RATE_SECONDS, TimeUnit.SECONDS);
                counter = new ImpressionCounter();
                uniqueKeysTracker = new UniqueKeysTrackerImp(telemetrySynchronizer, _config.uniqueKeysTimeToSend());
                processImpressionStrategy = new ProcessImpressionNone(_listener!=null, uniqueKeysTracker, counter);
                break;
        }
    }

    @Override
    public void track(List<Impression> impressions) {
        if (null == impressions) {
            return;
        }

        ImpressionsResult impressionsResult = processImpressionStrategy.process(impressions);
        List<Impression> impressionsForLogs = impressionsResult.getImpressionsToQueue();
        List<Impression> impressionsToListener = impressionsResult.getImpressionsToQueue();

        int totalImpressions = impressionsForLogs.size();
        long queued = _impressionsStorageProducer.put(impressionsForLogs.stream().map(KeyImpression::fromImpression).collect(Collectors.toList()));
        if (queued < totalImpressions) {
            _telemetryRuntimeProducer.recordImpressionStats(ImpressionsDataTypeEnum.IMPRESSIONS_DROPPED, totalImpressions-queued);
        }
        _telemetryRuntimeProducer.recordImpressionStats(ImpressionsDataTypeEnum.IMPRESSIONS_QUEUED, queued);

        if (_listener!=null){
            impressionsToListener.forEach(imp -> _listener.log(imp));
        }
    }

    @Override
    public void close() {
        try {
            if(_listener!= null){
                _listener.close();
                _log.info("Successful shutdown of ImpressionListener");
            }
            _scheduler.shutdown();
            sendImpressions();
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
        if (!counter.isEmpty()) {
            _impressionsSender.postCounters(counter.popAll());
        }
    }

    private ScheduledExecutorService buildExecutor() {
        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat("Split-ImpressionsManager-%d")
                .build();
        return Executors.newScheduledThreadPool(2, threadFactory);
    }

    @VisibleForTesting
    /* package private */ ImpressionCounter getCounter() {
        return counter;
    }
}
