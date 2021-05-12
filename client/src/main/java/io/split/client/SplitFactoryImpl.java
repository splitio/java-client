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
import io.split.engine.experiments.SplitChangeFetcher;
import io.split.engine.experiments.SplitFetcher;
import io.split.engine.experiments.SplitFetcherImp;
import io.split.engine.experiments.SplitParser;
import io.split.engine.experiments.SplitSynchronizationTask;
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

    private final URI _rootTarget;
    private final URI _eventsRootTarget;
    private final CloseableHttpClient _httpclient;
    private final SDKReadinessGates _gates;
    private final HttpMetrics _httpMetrics;
    private final FireAndForgetMetrics _unCachedFireAndForget;
    private final SegmentSynchronizationTaskImp _segmentSynchronizationTaskImp;
    private final SplitFetcher _splitFetcher;
    private final SplitSynchronizationTask _splitSynchronizationTask;
    private final ImpressionsManagerImpl _impressionsManager;
    private final FireAndForgetMetrics _cachedFireAndForgetMetrics;
    private final EventClient _eventClient;
    private final SyncManager _syncManager;
    private final Evaluator _evaluator;
    private final String _apiToken;

    // Caches
    private final SegmentCache _segmentCache;
    private final SplitCache _splitCache;

    // Client and Manager
    private final SplitClient _client;
    private final SplitManager _manager;

    private boolean isTerminated = false;
    private final ApiKeyCounter _apiKeyCounter;

    public SplitFactoryImpl(String apiToken, SplitClientConfig config) throws URISyntaxException {
        _apiToken = apiToken;
        _apiKeyCounter = ApiKeyCounter.getApiKeyCounterInstance();
        _apiKeyCounter.add(apiToken);

        if (config.blockUntilReady() == -1) {
            //BlockUntilReady not been set
            _log.warn("no setBlockUntilReadyTimeout parameter has been set - incorrect control treatments could be logged” " +
                    "if no ready config has been set when building factory");

        }

        // SDKReadinessGates
        _gates = new SDKReadinessGates();

        // HttpClient
        _httpclient = buildHttpClient(apiToken, config);

        // Roots
        _rootTarget = URI.create(config.endpoint());
        _eventsRootTarget = URI.create(config.eventsEndpoint());

        // HttpMetrics
        _httpMetrics = HttpMetrics.create(_httpclient, _eventsRootTarget);

        // Cache Initialisations
        _segmentCache = new SegmentCacheInMemoryImpl();
        _splitCache = new InMemoryCacheImp();

        // Metrics
        _unCachedFireAndForget = FireAndForgetMetrics.instance(_httpMetrics, 2, 1000);

        // Segments
        _segmentSynchronizationTaskImp = buildSegments(config);

        // SplitFetcher
        _splitFetcher = buildSplitFetcher();

        // SplitSynchronizationTask
        _splitSynchronizationTask = new SplitSynchronizationTask(_splitFetcher,
                _splitCache,
                findPollingPeriod(RANDOM, config.featuresRefreshRate()));

        // Impressions
        _impressionsManager = buildImpressionsManager(config);

        // CachedFireAndForgetMetrics
        _cachedFireAndForgetMetrics = buildCachedFireAndForgetMetrics(config);

        // EventClient
        _eventClient = EventClientImpl.create(_httpclient,
                _eventsRootTarget,
                config.eventsQueueSize(),
                config.eventFlushIntervalInMillis(),
                config.waitBeforeShutdown());

        // SyncManager
        _syncManager = SyncManagerImp.build(config.streamingEnabled(),
                _splitSynchronizationTask,
                _splitFetcher,
                _segmentSynchronizationTaskImp,
                _splitCache,
                config.authServiceURL(),
                _httpclient,
                config.streamingServiceURL(),
                config.authRetryBackoffBase(),
                buildSSEdHttpClient(config),
                _segmentCache,
                config.streamingRetryDelay(),
                config.streamingFetchMaxRetries(),
                config.failedAttemptsBeforeLogging(),
                config.cdnDebugLogging());
        _syncManager.start();

        // Evaluator
        _evaluator = new EvaluatorImp(_splitCache);

        // SplitClient
        _client = new SplitClientImpl(this,
                _splitCache,
                _impressionsManager,
                _cachedFireAndForgetMetrics,
                _eventClient,
                config,
                _gates,
                _evaluator);

        // SplitManager
        _manager = new SplitManagerImpl(_splitCache, config, _gates);

        // DestroyOnShutDown
        if (config.destroyOnShutDown()) {
            Thread shutdown = new Thread(() -> {
                // Using the full path to avoid conflicting with Thread.destroy()
                SplitFactoryImpl.this.destroy();
            });
            shutdown.setName("split-destroy-worker");
            Runtime.getRuntime().addShutdownHook(shutdown);
        }
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
    public synchronized void destroy() {
        if (!isTerminated) {
            _log.info("Shutdown called for split");
            try {
                _segmentSynchronizationTaskImp.close();
                _log.info("Successful shutdown of segment fetchers");
                _splitSynchronizationTask.close();
                _log.info("Successful shutdown of splits");
                _impressionsManager.close();
                _log.info("Successful shutdown of impressions manager");
                _unCachedFireAndForget.close();
                _log.info("Successful shutdown of metrics 1");
                _cachedFireAndForgetMetrics.close();
                _log.info("Successful shutdown of metrics 2");
                _httpclient.close();
                _log.info("Successful shutdown of httpclient");
                _eventClient.close();
                _log.info("Successful shutdown of eventClient");
                _syncManager.shutdown();
                _log.info("Successful shutdown of syncManager");
            } catch (IOException e) {
                _log.error("We could not shutdown split", e);
            }
            _apiKeyCounter.remove(_apiToken);
            isTerminated = true;
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

    private SegmentSynchronizationTaskImp buildSegments(SplitClientConfig config) throws URISyntaxException {
        SegmentChangeFetcher segmentChangeFetcher = HttpSegmentChangeFetcher.create(_httpclient, _rootTarget, _unCachedFireAndForget);

        return new SegmentSynchronizationTaskImp(segmentChangeFetcher,
                findPollingPeriod(RANDOM, config.segmentsRefreshRate()),
                config.numThreadsForSegmentFetch(),
                _gates,
                _segmentCache);
    }

    private SplitFetcher buildSplitFetcher() throws URISyntaxException {
        SplitChangeFetcher splitChangeFetcher = HttpSplitChangeFetcher.create(_httpclient, _rootTarget, _unCachedFireAndForget);
        SplitParser splitParser = new SplitParser(_segmentSynchronizationTaskImp, _segmentCache);

        return new SplitFetcherImp(splitChangeFetcher, splitParser, _gates, _splitCache);
    }

    private ImpressionsManagerImpl buildImpressionsManager(SplitClientConfig config) throws URISyntaxException {
        List<ImpressionListener> impressionListeners = new ArrayList<>();
        if (config.integrationsConfig() != null) {
            config.integrationsConfig().getImpressionsListeners(IntegrationsConfig.Execution.ASYNC).stream()
                    .map(l -> AsynchronousImpressionListener.build(l.listener(), l.queueSize()))
                    .collect(Collectors.toCollection(() -> impressionListeners));

            config.integrationsConfig().getImpressionsListeners(IntegrationsConfig.Execution.SYNC).stream()
                    .map(IntegrationsConfig.ImpressionListenerWithMeta::listener)
                    .collect(Collectors.toCollection(() -> impressionListeners));
        }

        return ImpressionsManagerImpl.instance(_httpclient, config, impressionListeners);
    }

    private FireAndForgetMetrics buildCachedFireAndForgetMetrics(SplitClientConfig config) {
        CachedMetrics cachedMetrics = new CachedMetrics(_httpMetrics, TimeUnit.SECONDS.toMillis(config.metricsRefreshRate()));

        return FireAndForgetMetrics.instance(cachedMetrics, 2, 1000);
    }
}
