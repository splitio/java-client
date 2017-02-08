package io.split.client.impressions;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.split.client.SplitClientConfig;
import io.split.client.dtos.KeyImpression;
import io.split.client.dtos.TestImpressions;
import io.split.engine.impressions.TreatmentLog;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Created by patricioe on 6/17/16.
 */
public class ImpressionsManager implements TreatmentLog, Runnable, Closeable {

    private static final Logger _logger = LoggerFactory.getLogger(ImpressionsManager.class);

    private final SplitClientConfig _config;
    private final CloseableHttpClient _client;
    private final BlockingQueue<KeyImpression> _queue;
    private final ScheduledExecutorService _scheduler;
    private final ImpressionsSender _impressionsSender;

    public static ImpressionsManager instance(CloseableHttpClient client,
                                              SplitClientConfig config) throws URISyntaxException {
        return new ImpressionsManager(client, config, null);
    }

    public static ImpressionsManager instanceForTest(CloseableHttpClient client,
                                                     SplitClientConfig config,
                                                     ImpressionsSender impressionsSender) throws URISyntaxException {
        return new ImpressionsManager(client, config, impressionsSender);
    }

    private ImpressionsManager(CloseableHttpClient client, SplitClientConfig config, ImpressionsSender impressionsSender) throws URISyntaxException {

        _config = config;
        _client = client;
        _queue = new ArrayBlockingQueue<KeyImpression>(config.impressionsQueueSize());

        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat("Split-ImpressionsManager-%d")
                .build();
        _scheduler = Executors.newSingleThreadScheduledExecutor(threadFactory);
        _scheduler.scheduleAtFixedRate(this, 10, config.impressionsRefreshRate(), TimeUnit.SECONDS);

        if (impressionsSender != null) {
            _impressionsSender = impressionsSender;
        } else {
            _impressionsSender = new HttpImpressionsSender(_client, config.eventsEndpoint());
        }
    }

    @Override
    public void log(String key, String bucketingKey, String feature, String treatment, long time, String label, Long changeNumber) {
        KeyImpression impression = keyImpression(feature, key, bucketingKey, treatment, time, label, changeNumber);
        _queue.offer(impression);
    }

    @Override
    public void close() throws IOException {
        _scheduler.shutdown();
        sendImpressions();
    }

    private KeyImpression keyImpression(String feature, String key, String bucketingKey, String treatment, long time, String label, Long changeNumber) {
        KeyImpression result = new KeyImpression();
        result.feature = feature;
        result.keyName = key;
        if (bucketingKey != null && !bucketingKey.equals(key)) {
            result.bucketingKey = bucketingKey;
        }
        result.label = label;
        result.treatment = treatment;
        result.time = time;
        result.changeNumber = changeNumber;
        return result;
    }

    @Override
    public void run() {
        sendImpressions();
    }

    private void sendImpressions() {

        if (_queue.remainingCapacity() == 0) {
            _logger.warn("Split SDK impressions queue is full. Impressions may have been dropped. Consider increasing capacity.");
        }

        long start = System.currentTimeMillis();

        List<KeyImpression> impressions = new ArrayList<>(_queue.size());
        _queue.drainTo(impressions);

        if (impressions.isEmpty()) {
            return; // Nothing to send
        }

        Map<String, List<KeyImpression>> tests = new HashMap<>();

        for (KeyImpression ki : impressions) {
            List<KeyImpression> impressionsForTest = tests.get(ki.feature);
            if (impressionsForTest == null) {
                impressionsForTest = new ArrayList<>();
                tests.put(ki.feature, impressionsForTest);
            }
            impressionsForTest.add(ki);
        }

        List<TestImpressions> toShip = Lists.newArrayList();

        for (Map.Entry<String, List<KeyImpression>> entry : tests.entrySet()) {
            String testName = entry.getKey();
            List<KeyImpression> keyImpressions = entry.getValue();

            TestImpressions testImpressionsDTO = new TestImpressions();
            testImpressionsDTO.testName = testName;
            testImpressionsDTO.keyImpressions = keyImpressions;

            toShip.add(testImpressionsDTO);
        }

        _impressionsSender.post(toShip);

        if(_config.debugEnabled()) {
            _logger.info(String.format("Posting %d Split impressions took %d millis",
                    impressions.size(), (System.currentTimeMillis() - start)));
        }
    }

}
