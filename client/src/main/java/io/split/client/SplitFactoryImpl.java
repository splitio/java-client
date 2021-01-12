package io.split.client;

import io.split.client.impressions.AsynchronousImpressionListener;
import io.split.client.impressions.ImpressionListener;
import io.split.client.impressions.ImpressionsManagerImpl;
import io.split.client.interceptors.AddSplitHeadersFilter;
import io.split.client.interceptors.GzipDecoderResponseInterceptor;
import io.split.client.interceptors.GzipEncoderRequestInterceptor;
import io.split.client.metrics.CachedMetrics;
import io.split.client.metrics.FireAndForgetMetrics;
import io.split.client.metrics.HttpMetrics;
import io.split.cache.InMemoryCacheImp;
import io.split.cache.SplitCache;
import io.split.engine.evaluator.Evaluator;
import io.split.engine.evaluator.EvaluatorImp;
import io.split.engine.SDKReadinessGates;
import io.split.engine.common.SyncManager;
import io.split.engine.common.SyncManagerImp;
import io.split.engine.experiments.SplitFetcherImp;
import io.split.engine.experiments.SplitSynchronizationTask;
import io.split.engine.experiments.SplitChangeFetcher;
import io.split.engine.experiments.SplitParser;
import io.split.engine.segments.SegmentChangeFetcher;
import io.split.cache.SegmentCache;
import io.split.cache.SegmentCacheInMemoryImpl;
import io.split.engine.segments.SegmentSynchronizationTaskImp;
import io.split.integrations.IntegrationsConfig;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.Credentials;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.cookie.StandardCookieSpec;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.impl.routing.DefaultProxyRoutePlanner;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.http.ssl.TLS;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class SplitFactoryImpl implements SplitFactory {
    private static final Logger _log = LoggerFactory.getLogger(SplitFactory.class);
    private final static long SSE_CONNECT_TIMEOUT = 30000;
    private final static long SSE_SOCKET_TIMEOUT = 70000;

    private static Random RANDOM = new Random();

    private final SplitClient _client;
    private final SplitManager _manager;
    private final Runnable destroyer;
    private final String _apiToken;
    private boolean isTerminated = false;
    private final FactoryInstantiationsCounter _factoryInstantiationsCounter;

    public SplitFactoryImpl(String apiToken, SplitClientConfig config) throws URISyntaxException {
        _apiToken = apiToken;
        _factoryInstantiationsCounter = FactoryInstantiationsCounter.getFactoryInstantiationsServiceInstance();
        _factoryInstantiationsCounter.addToken(apiToken);

        if (config.blockUntilReady() == -1) {
            //BlockUntilReady not been set
            _log.warn("no setBlockUntilReadyTimeout parameter has been set - incorrect control treatments could be logged” " +
                    "if no ready config has been set when building factory");

        }

        final CloseableHttpClient httpclient = buildHttpClient(apiToken, config);

        URI rootTarget = URI.create(config.endpoint());
        URI eventsRootTarget = URI.create(config.eventsEndpoint());

        // Metrics
        HttpMetrics httpMetrics = HttpMetrics.create(httpclient, eventsRootTarget);
        final FireAndForgetMetrics uncachedFireAndForget = FireAndForgetMetrics.instance(httpMetrics, 2, 1000);

        SDKReadinessGates gates = new SDKReadinessGates();

        // Segments
        SegmentChangeFetcher segmentChangeFetcher = HttpSegmentChangeFetcher.create(httpclient, rootTarget, uncachedFireAndForget);
        //This segmentCache is for inMemory Storage (the only one supported by java-client for the moment
        SegmentCache segmentCache = new SegmentCacheInMemoryImpl();
        final SegmentSynchronizationTaskImp segmentSynchronizationTaskImp = new SegmentSynchronizationTaskImp(segmentChangeFetcher,
                findPollingPeriod(RANDOM, config.segmentsRefreshRate()),
                config.numThreadsForSegmentFetch(),
                gates,
                segmentCache);

        SplitParser splitParser = new SplitParser(segmentSynchronizationTaskImp, segmentCache);

        // Feature Changes
        SplitChangeFetcher splitChangeFetcher = HttpSplitChangeFetcher.create(httpclient, rootTarget, uncachedFireAndForget);

        final SplitCache splitCache = new InMemoryCacheImp();
        final SplitFetcherImp splitFetcher = new SplitFetcherImp(splitChangeFetcher, splitParser, gates, splitCache);
        final SplitSynchronizationTask splitSynchronizationTask = new SplitSynchronizationTask(splitFetcher, splitCache, findPollingPeriod(RANDOM, config.featuresRefreshRate()));

        List<ImpressionListener> impressionListeners = new ArrayList<>();
        // Setup integrations
        if (config.integrationsConfig() != null) {
            config.integrationsConfig().getImpressionsListeners(IntegrationsConfig.Execution.ASYNC).stream()
                    .map(l -> AsynchronousImpressionListener.build(l.listener(), l.queueSize()))
                    .collect(Collectors.toCollection(() -> impressionListeners));

            config.integrationsConfig().getImpressionsListeners(IntegrationsConfig.Execution.SYNC).stream()
                    .map(IntegrationsConfig.ImpressionListenerWithMeta::listener)
                    .collect(Collectors.toCollection(() -> impressionListeners));
        }

        // Impressions
        final ImpressionsManagerImpl impressionsManager = ImpressionsManagerImpl.instance(httpclient, config, impressionListeners);

        CachedMetrics cachedMetrics = new CachedMetrics(httpMetrics, TimeUnit.SECONDS.toMillis(config.metricsRefreshRate()));
        final FireAndForgetMetrics cachedFireAndForgetMetrics = FireAndForgetMetrics.instance(cachedMetrics, 2, 1000);

        final EventClient eventClient = EventClientImpl.create(httpclient, eventsRootTarget, config.eventsQueueSize(), config.eventFlushIntervalInMillis(), config.waitBeforeShutdown());

        // SyncManager
        final SyncManager syncManager = SyncManagerImp.build(config.streamingEnabled(), splitSynchronizationTask, splitFetcher, segmentSynchronizationTaskImp, splitCache, config.authServiceURL(), httpclient, config.streamingServiceURL(), config.authRetryBackoffBase(), buildSSEdHttpClient(config), segmentCache);
        syncManager.start();

        // Evaluator
        final Evaluator evaluator = new EvaluatorImp(splitCache);

        destroyer = new Runnable() {
            public void run() {
                _log.info("Shutdown called for split");
                try {
                    segmentSynchronizationTaskImp.close();
                    _log.info("Successful shutdown of segment fetchers");
                    splitSynchronizationTask.close();
                    _log.info("Successful shutdown of splits");
                    impressionsManager.close();
                    _log.info("Successful shutdown of impressions manager");
                    uncachedFireAndForget.close();
                    _log.info("Successful shutdown of metrics 1");
                    cachedFireAndForgetMetrics.close();
                    _log.info("Successful shutdown of metrics 2");
                    httpclient.close();
                    _log.info("Successful shutdown of httpclient");
                    eventClient.close();
                    _log.info("Successful shutdown of eventClient");
                    new Thread(syncManager::shutdown).start();
                    _log.info("Successful shutdown of syncManager");
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
                splitCache,
                impressionsManager,
                cachedFireAndForgetMetrics,
                eventClient,
                config,
                gates,
                evaluator);
        _manager = new SplitManagerImpl(splitCache, config, gates);
    }

    @Override
    public SplitClient client() {
        return _client;
    }

    @Override
    public SplitManager manager() {
        return _manager;
    }

    @Override
    public void destroy() {
        synchronized (SplitFactoryImpl.class) {
            if (!isTerminated) {
                destroyer.run();
                _factoryInstantiationsCounter.removeToken(_apiToken);
                int i = FactoryInstantiationsCounter.getFactoryInstantiationsServiceInstance().getCount(_apiToken);
                isTerminated = true;
            }
        }
    }

    @Override
    public boolean isDestroyed() {
        return isTerminated;
    }

    private static CloseableHttpClient buildHttpClient(String apiToken, SplitClientConfig config) {

        SSLConnectionSocketFactory sslSocketFactory = SSLConnectionSocketFactoryBuilder.create()
                .setSslContext(SSLContexts.createSystemDefault())
                .setTlsVersions(TLS.V_1_1, TLS.V_1_2)
                .build();

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(config.connectionTimeout()))
                .setCookieSpec(StandardCookieSpec.STRICT)
                .build();

        PoolingHttpClientConnectionManager cm = PoolingHttpClientConnectionManagerBuilder.create()
                .setSSLSocketFactory(sslSocketFactory)
                .setDefaultSocketConfig(SocketConfig.custom()
                        .setSoTimeout(Timeout.ofMilliseconds(config.readTimeout()))
                        .build())
                .build();
        cm.setMaxTotal(20);
        cm.setDefaultMaxPerRoute(20);

        HttpClientBuilder httpClientbuilder = HttpClients.custom()
                .setConnectionManager(cm)
                .setDefaultRequestConfig(requestConfig)
                .addRequestInterceptorLast(AddSplitHeadersFilter.instance(apiToken, config.ipAddressEnabled()))
                .addRequestInterceptorLast(new GzipEncoderRequestInterceptor())
                .addResponseInterceptorLast((new GzipDecoderResponseInterceptor()));

        // Set up proxy is it exists
        if (config.proxy() != null) {
            httpClientbuilder = setupProxy(httpClientbuilder, config);
        }

        return httpClientbuilder.build();
    }

    private static CloseableHttpClient buildSSEdHttpClient(SplitClientConfig config) {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(SSE_CONNECT_TIMEOUT))
                .build();

        SSLConnectionSocketFactory sslSocketFactory = SSLConnectionSocketFactoryBuilder.create()
                .setSslContext(SSLContexts.createSystemDefault())
                .setTlsVersions(TLS.V_1_1, TLS.V_1_2)
                .build();

        PoolingHttpClientConnectionManager cm = PoolingHttpClientConnectionManagerBuilder.create()
                .setSSLSocketFactory(sslSocketFactory)
                .setDefaultSocketConfig(SocketConfig.custom()
                        .setSoTimeout(Timeout.ofMilliseconds(SSE_SOCKET_TIMEOUT))
                        .build())
                .build();
        cm.setMaxTotal(1);
        cm.setDefaultMaxPerRoute(1);

        HttpClientBuilder httpClientbuilder = HttpClients.custom()
                .setConnectionManager(cm)
                .setDefaultRequestConfig(requestConfig);

        // Set up proxy is it exists
        if (config.proxy() != null) {
            httpClientbuilder = setupProxy(httpClientbuilder, config);
        }

        return httpClientbuilder.build();
    }

    private static HttpClientBuilder setupProxy(HttpClientBuilder httpClientbuilder, SplitClientConfig config) {
        _log.info("Initializing Split SDK with proxy settings");
        DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(config.proxy());
        httpClientbuilder.setRoutePlanner(routePlanner);

        if (config.proxyUsername() != null && config.proxyPassword() != null) {
            _log.debug("Proxy setup using credentials");
            BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();
            AuthScope siteScope = new AuthScope(config.proxy().getHostName(), config.proxy().getPort());
            Credentials siteCreds = new UsernamePasswordCredentials(config.proxyUsername(), config.proxyPassword().toCharArray());
            credsProvider.setCredentials(siteScope, siteCreds);
            httpClientbuilder.setDefaultCredentialsProvider(credsProvider);
        }

        return  httpClientbuilder;
    }

    private static int findPollingPeriod(Random rand, int max) {
        int min = max / 2;
        return rand.nextInt((max - min) + 1) + min;
    }
}
