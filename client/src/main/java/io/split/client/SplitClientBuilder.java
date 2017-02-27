package io.split.client;

import io.split.client.impressions.ImpressionsManager;
import io.split.client.interceptors.AddSplitHeadersFilter;
import io.split.client.interceptors.GzipDecoderResponseInterceptor;
import io.split.client.interceptors.GzipEncoderRequestInterceptor;
import io.split.client.jmx.JmxMonitor;
import io.split.client.jmx.SplitJmxMonitor;
import io.split.client.metrics.CachedMetrics;
import io.split.client.metrics.FireAndForgetMetrics;
import io.split.client.metrics.HttpMetrics;
import io.split.engine.SDKReadinessGates;
import io.split.engine.experiments.RefreshableSplitFetcherProvider;
import io.split.engine.experiments.SplitChangeFetcher;
import io.split.engine.experiments.SplitFetcher;
import io.split.engine.experiments.SplitParser;
import io.split.engine.impressions.TreatmentLog;
import io.split.engine.segments.RefreshableSegmentFetcher;
import io.split.engine.segments.SegmentChangeFetcher;
import io.split.engine.segments.SegmentFetcher;
import io.split.grammar.Treatments;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Builds an instance of SplitClient.
 *
 * @Deprecated use SplitFactoryBuilder instead. This class will be removed in future releases.
 */
@Deprecated
public class SplitClientBuilder {

    private static final Logger _log = LoggerFactory.getLogger(SplitClientBuilder.class);

    private static Random RANDOM = new Random();

    private static AtomicReference<SplitClient> _client = new AtomicReference<SplitClient>();
    private static Object _lock = new Object();

    /**
     * Instantiates a SplitClient with default configurations
     *
     * @param apiToken the API token. MUST NOT be null
     * @return a SplitClient
     *
     * @throws IOException                           if the SDK was being started in 'localhost' mode, but
     *                                               there were problems reading the override file from disk.
     * @throws java.lang.InterruptedException        if you asked to block until the sdk was
     *                                               ready and the block was interrupted.
     * @throws java.util.concurrent.TimeoutException if you asked to block until the sdk was
     *                                               ready and the timeout specified via config#ready() passed.
     *
     * @Deprecated use SplitFactory.build(apiToken).client() instead
     */
    @Deprecated
    public static SplitClient build(String apiToken) throws IOException, InterruptedException, TimeoutException, URISyntaxException {
        return build(apiToken, SplitClientConfig.builder().build());
    }

    /**
     * @param apiToken the API token. MUST NOT be null
     * @param config   parameters to control sdk construction. MUST NOT be null.
     * @return a SplitClient
     * @throws IOException                           if the SDK was being started in 'localhost' mode, but
     *                                               there were problems reading the override file from disk.
     * @throws java.lang.InterruptedException        if you asked to block until the sdk was
     *                                               ready and the block was interrupted.
     * @throws java.util.concurrent.TimeoutException if you asked to block until the sdk was
     *                                               ready and the timeout specified via config#ready() passed.
     *
     * @Deprecated use SplitFactory.build(apiToken, config).client() instead
     */
    @Deprecated
    public static SplitClient build(String apiToken, SplitClientConfig config) throws IOException, InterruptedException, TimeoutException, URISyntaxException {
        if (_client.get() != null) {
            return _client.get();
        }

        synchronized (_lock) {
            // double check locking.
            if (_client.get() != null) {
                return _client.get();
            }

            if (LocalhostSplitClientBuilder.LOCALHOST.equals(apiToken)) {
                SplitClient splitClient = LocalhostSplitClientBuilder.build();
                _client.set(splitClient);
                registerJmxMonitor(splitClient);
                return splitClient;
            }

            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(config.connectionTimeout())
                    .setSocketTimeout(config.readTimeout())
                    .build();

            PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
            cm.setMaxTotal(20);
            cm.setDefaultMaxPerRoute(20);
            final CloseableHttpClient httpclient = HttpClients.custom()
                    .setConnectionManager(cm)
                    .setDefaultRequestConfig(requestConfig)
                    .addInterceptorLast(AddSplitHeadersFilter.instance(apiToken))
                    .addInterceptorLast(new GzipEncoderRequestInterceptor())
                    .addInterceptorLast(new GzipDecoderResponseInterceptor())
                    .build();

            URI rootTarget = URI.create(config.endpoint());
            URI eventsRootTarget = URI.create(config.eventsEndpoint());

            // Metrics
            HttpMetrics httpMetrics = HttpMetrics.create(httpclient, eventsRootTarget);
            final FireAndForgetMetrics uncachedFireAndForget = FireAndForgetMetrics.instance(httpMetrics, 2, 1000);

            SDKReadinessGates gates = new SDKReadinessGates();

            // Segments
            SegmentChangeFetcher segmentChangeFetcher = HttpSegmentChangeFetcher.create(httpclient, rootTarget, uncachedFireAndForget);
            final RefreshableSegmentFetcher segmentFetcher = new RefreshableSegmentFetcher(segmentChangeFetcher,
                    findPollingPeriod(RANDOM, config.segmentsRefreshRate()),
                    config.numThreadsForSegmentFetch(),
                    gates);


            SplitParser splitParser = new SplitParser(segmentFetcher);

            // Feature Changes
            SplitChangeFetcher splitChangeFetcher = HttpSplitChangeFetcher.create(httpclient, rootTarget, uncachedFireAndForget);

            final RefreshableSplitFetcherProvider splitFetcherProvider = new RefreshableSplitFetcherProvider(splitChangeFetcher, splitParser, findPollingPeriod(RANDOM, config.featuresRefreshRate()), gates);

            // Impressions
            final TreatmentLog treatmentLog = ImpressionsManager.instance(httpclient, config);

            CachedMetrics cachedMetrics = new CachedMetrics(httpMetrics, TimeUnit.SECONDS.toMillis(config.metricsRefreshRate()));
            final FireAndForgetMetrics cachedFireAndForgetMetrics = FireAndForgetMetrics.instance(cachedMetrics, 2, 1000);

            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    _log.warn("Shutdown called for split");
                    try {
                        segmentFetcher.close();
                        _log.warn("Successful shutdown of segment fetchers");
                        splitFetcherProvider.close();
                        _log.warn("Successful shutdown of splits");
                        uncachedFireAndForget.close();
                        _log.warn("Successful shutdown of metrics 1");
                        cachedFireAndForgetMetrics.close();
                        _log.warn("Successful shutdown of metrics 2");
                        ((ImpressionsManager) treatmentLog).close();
                        _log.warn("Successful shutdown of ImpressionManager");
                        httpclient.close();
                        _log.warn("Successful shutdown of httpclient");
                    } catch (IOException e) {
                        _log.error("We could not shutdown split", e);
                    }
                }

            });

            // Now create the client.
            SplitClient splitClient = new SplitClientImpl(splitFetcherProvider.getFetcher(), treatmentLog, cachedFireAndForgetMetrics, config);

            registerJmxMonitor(splitClient, splitFetcherProvider.getFetcher(), segmentFetcher);

            _client.set(splitClient);

            if (config.blockUntilReady() > 0) {
                if (!gates.isSDKReady(config.blockUntilReady())) {
                    throw new TimeoutException("SDK was not ready in " + config.blockUntilReady() + " milliseconds");
                }
            }
            return splitClient;
        }

    }

    private static void registerJmxMonitor(SplitClient splitClient) {
        registerJmxMonitor(splitClient, null, null);
    }

    private static void registerJmxMonitor(SplitClient splitClient, SplitFetcher fetcher, SegmentFetcher segmentFetcher) {
        try {
            SplitJmxMonitor mbean = new SplitJmxMonitor(splitClient, fetcher, segmentFetcher);
            JmxMonitor.getInstance().registerMonitor("io.split.monitor", "Split", mbean);
        } catch (Exception e) {
            _log.warn("Unable to create JMX monitor", e);
        }
    }

    private static int findPollingPeriod(Random rand, int max) {
        int min = max / 2;
        return rand.nextInt((max - min) + 1) + min;
    }


    public static void main(String... args) throws IOException, InterruptedException, TimeoutException, URISyntaxException {

        if (args.length != 1) {
            System.out.println("Usage: <api_token>");
            System.exit(1);
            return;
        }

        SplitClientConfig config = SplitClientConfig.builder()
                .endpoint("http://localhost:8081", "http://localhost:8081")
                .enableDebug()
                .build();

        SplitClient client = SplitClientBuilder.build(args[0], config);

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                if ("exit".equals(line)) {
                    System.exit(0);
                }
                String[] userIdAndSplit = line.split(" ");

                if (userIdAndSplit.length != 2) {
                    System.out.println("Could not understand command");
                    continue;
                }

                boolean isOn = client.getTreatment(userIdAndSplit[0], userIdAndSplit[1]).equals("on");

                System.out.println(isOn ? Treatments.ON : Treatments.OFF);
            }

        } catch (IOException io) {
            _log.error(io.getMessage(), io);
        }
    }
}
