package io.split.client;

import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.Multiset;
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
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class SplitFactoryImpl implements SplitFactory {
    private static final Logger _log = LoggerFactory.getLogger(SplitFactory.class);

    private static final Multiset<String> USED_API_TOKENS = ConcurrentHashMultiset.create();
    private static Random RANDOM = new Random();

    private final SplitClient _client;
    private final SplitManager _manager;
    private final Runnable destroyer;
    private final String _apiToken;
    private boolean isTerminated = false;

    public SplitFactoryImpl(String apiToken, SplitClientConfig config) throws URISyntaxException {
        _apiToken = apiToken;
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
                new String[]{"TLSv1.1", "TLSv1.2"},
                null,
                SSLConnectionSocketFactory.getDefaultHostnameVerifier());

        Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", sslsf)
                .build();

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(config.connectionTimeout())
                .setSocketTimeout(config.readTimeout())
                .setCookieSpec(CookieSpecs.STANDARD)
                .build();

        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(registry);
        cm.setMaxTotal(20);
        cm.setDefaultMaxPerRoute(20);

        HttpClientBuilder httpClientbuilder = HttpClients.custom()
                .setConnectionManager(cm)
                .setDefaultRequestConfig(requestConfig)
                .setSSLSocketFactory(sslsf)
                .addInterceptorLast(AddSplitHeadersFilter.instance(apiToken))
                .addInterceptorLast(new GzipEncoderRequestInterceptor())
                .addInterceptorLast(new GzipDecoderResponseInterceptor());


        if (USED_API_TOKENS.contains(apiToken)) {
            String message = String.format("factory instantiation: You are instantiating a Split factory with " +
                    "the exact same API Key you’ve used before. " +
                    "You already have %s instances with the same API Key. " +
                    "We recommend keeping only one instance of the factory at all times (Singleton pattern) " +
                    "and reusing it throughout your application.", USED_API_TOKENS.count(apiToken));
            _log.warn(message);
        } else if (!USED_API_TOKENS.isEmpty()) {
            String message = "factory instantiation: You already have an instance of the Split factory on this machine. " +
                    "Make sure you definitely want this additional instance. " +
                    "We recommend keeping only one instance of the factory at all times (Singleton pattern) " +
                    "and reusing it throughout your application.";
            _log.warn(message);
        }
        USED_API_TOKENS.add(apiToken);

        if (config.blockUntilReady() == -1) {
            //BlockUntilReady not been set
            _log.warn("no setBlockUntilReadyTimeout parameter has been set - incorrect control treatments could be logged” " +
                    "if no ready config has been set when building factory");

        }
        // Set up proxy is it exists
        if (config.proxy() != null) {
            _log.info("Initializing Split SDK with proxy settings");
            DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(config.proxy());
            httpClientbuilder.setRoutePlanner(routePlanner);

            if (config.proxyUsername() != null && config.proxyPassword() != null){
                _log.debug("Proxy setup using credentials");
                CredentialsProvider credsProvider = new BasicCredentialsProvider();
                AuthScope siteScope = new AuthScope(config.proxy().getHostName(), config.proxy().getPort());
                Credentials siteCreds = new UsernamePasswordCredentials(config.proxyUsername(), config.proxyPassword());
                credsProvider.setCredentials(siteScope, siteCreds);

                httpClientbuilder.setDefaultCredentialsProvider(credsProvider);
            }
        }

        final CloseableHttpClient httpclient = httpClientbuilder.build();

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

        final EventClient eventClient = EventClientImpl.create(httpclient, eventsRootTarget, config.eventsQueueSize(), config.eventFlushIntervalInMillis(), config.waitBeforeShutdown());

        destroyer = new Runnable() {
            public void run() {
                _log.info("Shutdown called for split");
                try {
                    segmentFetcher.close();
                    _log.info("Successful shutdown of segment fetchers");
                    splitFetcherProvider.close();
                    _log.info("Successful shutdown of splits");
                    uncachedFireAndForget.close();
                    _log.info("Successful shutdown of metrics 1");
                    cachedFireAndForgetMetrics.close();
                    _log.info("Successful shutdown of metrics 2");
                    impressionListener.close();
                    _log.info("Successful shutdown of ImpressionListener");
                    httpclient.close();
                    _log.info("Successful shutdown of httpclient");
                    eventClient.close();
                    _log.info("Successful shutdown of httpclient");
                } catch (IOException e) {
                    _log.error("We could not shutdown split", e);
                }
            }
        };

        if (config.destroyOnShutDown()) {
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    // Using the full path to avoid conflicting with Thread.destroy()
                    SplitFactoryImpl.this.destroy();
                }
            });
        }

        _client = new SplitClientImpl(this,
                splitFetcherProvider.getFetcher(),
                impressionListener,
                cachedFireAndForgetMetrics,
                eventClient,
                config,
                gates);
        _manager = new SplitManagerImpl(splitFetcherProvider.getFetcher(), config, gates);
    }

    private static int findPollingPeriod(Random rand, int max) {
        int min = max / 2;
        return rand.nextInt((max - min) + 1) + min;
    }

    public SplitClient client() {
        return _client;
    }

    public SplitManager manager() {
        return _manager;
    }

    public void destroy() {
        synchronized (SplitFactoryImpl.class) {
            if (!isTerminated) {
                destroyer.run();
                USED_API_TOKENS.remove(_apiToken);
                isTerminated = true;
            }
        }
    }

    @Override
    public boolean isDestroyed() {
        return isTerminated;
    }
}
