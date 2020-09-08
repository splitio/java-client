package io.split.client.impressions;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.split.client.SplitClientConfig;
import io.split.client.dtos.KeyImpression;
import io.split.client.dtos.TestImpressions;
import org.apache.http.impl.client.CloseableHttpClient;
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

/**
 * Created by patricioe on 6/17/16.
 */
public class ImpressionsManagerImpl implements ImpressionsManager, Runnable, Closeable {

    private static final Logger _log = LoggerFactory.getLogger(ImpressionsManagerImpl.class);
    private static final long LAST_SEEN_CACHE_SIZE = 500000; // cache up to 500k impression hashes
    private static final long TIMEFRAME_MS = 3600 * 1000;

    private final SplitClientConfig _config;
    private final CloseableHttpClient _client;
    private final ImpressionsStorage _storage;
    private final ScheduledExecutorService _scheduler;
    private final ImpressionsSender _impressionsSender;
    private final ImpressionObserver _impressionObserver;
    private final ImpressionListener _listener;

    public static ImpressionsManagerImpl instance(CloseableHttpClient client,
                                                  SplitClientConfig config,
                                                  List<ImpressionListener> listeners) throws URISyntaxException {
        return new ImpressionsManagerImpl(client, config, null, listeners);
    }

    public static ImpressionsManagerImpl instanceForTest(CloseableHttpClient client,
                                                         SplitClientConfig config,
                                                         ImpressionsSender impressionsSender,
                                                         List<ImpressionListener> listeners) throws URISyntaxException {
        return new ImpressionsManagerImpl(client, config, impressionsSender, listeners);
    }

    private ImpressionsManagerImpl(CloseableHttpClient client,
                                   SplitClientConfig config,
                                   ImpressionsSender impressionsSender,
                                   List<ImpressionListener> listeners) throws URISyntaxException {

        _config = config;
        _client = client;
        _storage = new InMemoryImpressionsStorage(config.impressionsQueueSize());
        _impressionObserver = new ImpressionObserver(LAST_SEEN_CACHE_SIZE);

        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat("Split-ImpressionsManager-%d")
                .build();
        _scheduler = Executors.newSingleThreadScheduledExecutor(threadFactory);
        _scheduler.scheduleAtFixedRate(this, 10, config.impressionsRefreshRate(), TimeUnit.SECONDS);

        if (impressionsSender != null) {
            _impressionsSender = impressionsSender;
        } else {
            _impressionsSender = HttpImpressionsSender.create(_client, URI.create(config.eventsEndpoint()));
        }

        _listener = (null != listeners && !listeners.isEmpty()) ? new ImpressionListener.FederatedImpressionListener(listeners)
                : new ImpressionListener.NoopImpressionListener();
    }

    private static boolean shouldQueueImpression(Impression i) {
        return Objects.isNull(i.pt()) || (i.pt() < (System.currentTimeMillis() - TIMEFRAME_MS));
    }

    @Override
    public void track(Impression impression) {
        // TODO: Increment count
        impression = impression.withPreviousTime(_impressionObserver.testAndSet(impression));
        _listener.log(impression);
        if (shouldQueueImpression(impression)) {
            _storage.put(KeyImpression.fromImpression(impression));
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

    @Override
    public void run() {
        sendImpressions();
    }

    private void sendImpressions() {
        if (_storage.isFull()) {
            _log.warn("Split SDK impressions queue is full. Impressions may have been dropped. Consider increasing capacity.");
        }

        long start = System.currentTimeMillis();
        List<KeyImpression> impressions = _storage.pop();
        if (impressions.isEmpty()) {
            return; // Nothing to send
        }

        _impressionsSender.post(TestImpressions.fromKeyImpressions(impressions));
        if(_config.debugEnabled()) {
            _log.info(String.format("Posting %d Split impressions took %d millis",
                    impressions.size(), (System.currentTimeMillis() - start)));
        }
    }
}
