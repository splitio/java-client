package io.split.client;

import io.split.client.impressions.AsynchronousImpressionListener;
import io.split.client.impressions.ImpressionListener;
import io.split.client.impressions.ImpressionsManager;
import io.split.client.interceptors.AddSplitHeadersFilter;
import io.split.client.interceptors.GzipDecoderResponseInterceptor;
import io.split.client.interceptors.GzipEncoderRequestInterceptor;
import io.split.client.metrics.CachedMetrics;
import io.split.client.metrics.FireAndForgetMetrics;
import io.split.client.metrics.HttpMetrics;
import io.split.engine.SDKReadinessGates;
import io.split.engine.experiments.RefreshableSplitFetcherProvider;
import io.split.engine.experiments.SplitChangeFetcher;
import io.split.engine.experiments.SplitParser;
import io.split.engine.segments.RefreshableSegmentFetcher;
import io.split.engine.segments.SegmentChangeFetcher;
import io.split.grammar.Treatments;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Builds an instance of SplitClient.
 */
public class SplitFactoryBuilder {

    private static final Logger _log = LoggerFactory.getLogger(SplitFactoryBuilder.class);

    private static Random RANDOM = new Random();

    /**
     * Instantiates a SplitFactory with default configurations
     *
     * @param apiToken the API token. MUST NOT be null
     * @return a SplitFactory
     * @throws IOException                           if the SDK was being started in 'localhost' mode, but
     *                                               there were problems reading the override file from disk.
     * @throws java.lang.InterruptedException        if you asked to block until the sdk was
     *                                               ready and the block was interrupted.
     * @throws java.util.concurrent.TimeoutException if you asked to block until the sdk was
     *                                               ready and the timeout specified via config#ready() passed.
     */
    public static SplitFactory build(String apiToken) throws IOException, InterruptedException, TimeoutException, URISyntaxException {
        return build(apiToken, SplitClientConfig.builder().build());
    }

    /**
     * @param apiToken the API token. MUST NOT be null
     * @param config   parameters to control sdk construction. MUST NOT be null.
     * @return a SplitFactory
     * @throws java.io.IOException                   if the SDK was being started in 'localhost' mode, but
     *                                               there were problems reading the override file from disk.
     * @throws InterruptedException                  if you asked to block until the sdk was
     *                                               ready and the block was interrupted.
     * @throws java.util.concurrent.TimeoutException if you asked to block until the sdk was
     *                                               ready and the timeout specified via config#ready() passed.
     */
    public static synchronized SplitFactory build(String apiToken, SplitClientConfig config) throws IOException, InterruptedException, TimeoutException, URISyntaxException {
        if (LocalhostSplitFactory.LOCALHOST.equals(apiToken)) {
            return LocalhostSplitFactory.createLocalhostSplitFactory();
        }

        SSLContext sslContext = null;
        try {
            sslContext = SSLContexts.custom()
                    .useTLS()
                    .build();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException("Unable to create support for secure connection.");
        }

        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
                sslContext,
                new String[]{"TLSv1.1","TLSv1.2"},
                null,
                SSLConnectionSocketFactory.getDefaultHostnameVerifier());

        Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", sslsf)
                .build();

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(config.connectionTimeout())
                .setSocketTimeout(config.readTimeout())
                .build();

        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(registry);
        cm.setMaxTotal(20);
        cm.setDefaultMaxPerRoute(20);

        final CloseableHttpClient httpclient = HttpClients.custom()
                .setConnectionManager(cm)
                .setDefaultRequestConfig(requestConfig)
                .setSSLSocketFactory(sslsf)
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
        final ImpressionsManager splitImpressionListener = ImpressionsManager.instance(httpclient, config);
        final ImpressionListener impressionListener;

        if (config.impressionListener() != null) {
            AsynchronousImpressionListener wrapper = AsynchronousImpressionListener.build(config.impressionListener(), config.impressionListenerCapactity());
            List<ImpressionListener> impressionListeners = new ArrayList<ImpressionListener>();
            impressionListeners.add(splitImpressionListener);
            impressionListeners.add(wrapper);
            impressionListener = new ImpressionListener.FederatedImpressionListener(impressionListeners);
        } else {
            impressionListener = splitImpressionListener;
        }

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
                    impressionListener.close();
                    _log.warn("Successful shutdown of ImpressionListener");
                    httpclient.close();
                    _log.warn("Successful shutdown of httpclient");
                } catch (IOException e) {
                    _log.error("We could not shutdown split", e);
                }
            }

        });

        // Now create the client.
        SplitFactory splitFactory = new SplitFactoryImpl(splitFetcherProvider.getFetcher(), impressionListener, cachedFireAndForgetMetrics, config);


        if (config.blockUntilReady() > 0) {
            if (!gates.isSDKReady(config.blockUntilReady())) {
                throw new TimeoutException("SDK was not ready in " + config.blockUntilReady() + " milliseconds");
            }
        }

        return splitFactory;
    }

    /**
     * Instantiates a local Off-The-Grid SplitFactory
     *
     * @return a SplitFactory
     * @throws IOException if there were problems reading the override file from disk.
     */
    public static SplitFactory local() throws IOException {
        return LocalhostSplitFactory.createLocalhostSplitFactory();
    }

    /**
     * Instantiates a local Off-The-Grid SplitFactory
     *
     * @param home A directory containing the .split file from which to build treatments. MUST NOT be null
     *
     * @return a SplitFactory
     * @throws IOException if there were problems reading the override file from disk.
     */
    public static SplitFactory local(String home) throws IOException {
        return new LocalhostSplitFactory(home);
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

        ImpressionListener twilioImpressionListener = null;

        SplitClientConfig config = SplitClientConfig.builder()
                .impressionListener(twilioImpressionListener, 10)
                .build();

        SplitClient client = SplitFactoryBuilder.build("API_KEY", config).client();

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
