package io.split.client;

import io.split.client.events.EventsStorage;
import io.split.client.events.EventsTask;
import io.split.client.events.InMemoryEventsStorage;
import io.split.client.impressions.AsynchronousImpressionListener;
import io.split.client.impressions.ImpressionListener;
import io.split.client.impressions.ImpressionsManagerImpl;
import io.split.client.interceptors.AuthorizationInterceptorFilter;
import io.split.client.interceptors.ClientKeyInterceptorFilter;
import io.split.client.interceptors.GzipDecoderResponseInterceptor;
import io.split.client.interceptors.GzipEncoderRequestInterceptor;
import io.split.client.interceptors.SdkMetadataInterceptorFilter;
import io.split.client.utils.SDKMetadata;
import io.split.storages.memory.InMemoryCacheImp;
import io.split.storages.SplitCache;
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
import io.split.storages.SegmentCache;
import io.split.storages.memory.SegmentCacheInMemoryImpl;
import io.split.engine.segments.SegmentSynchronizationTaskImp;
import io.split.integrations.IntegrationsConfig;
import io.split.storages.pluggable.CustomStorageWrapper;
import io.split.storages.pluggable.adapters.UserCustomTelemetryAdapterProducer;
import io.split.telemetry.storage.InMemoryTelemetryStorage;
import io.split.telemetry.storage.NoopTelemetryStorage;
import io.split.telemetry.storage.TelemetryStorage;
import io.split.telemetry.synchronizer.TelemetryInMemorySubmitter;
import io.split.telemetry.synchronizer.TelemetrySyncTask;
import io.split.telemetry.synchronizer.TelemetrySynchronizer;
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
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
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
    private final SegmentSynchronizationTaskImp _segmentSynchronizationTaskImp;
    private final SplitFetcher _splitFetcher;
    private final SplitSynchronizationTask _splitSynchronizationTask;
    private final ImpressionsManagerImpl _impressionsManager;
    private final EventsStorage _eventsStorage;
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
    private final TelemetryStorageProducer _telemetryStorageProducer;
    private final TelemetryStorageConsumer _telemetryStorageConsumer;
    private final TelemetrySynchronizer _telemetrySynchronizer;
    private final TelemetrySyncTask _telemetrySyncTask;
    private final long _startTime;
    private final EventsTask _eventsTask;
    private final SDKMetadata _sdkMetadata;


    //Constructor for standalone mode
    public SplitFactoryImpl(String apiToken, SplitClientConfig config) throws URISyntaxException {
        _startTime = System.currentTimeMillis();
        _apiToken = apiToken;
        _apiKeyCounter = ApiKeyCounter.getApiKeyCounterInstance();
        _apiKeyCounter.add(apiToken);
        _sdkMetadata = createSdkMetadata(config.ipAddressEnabled(), SplitClientConfig.splitSdkVersion);

        TelemetryStorage telemetryStorage = new InMemoryTelemetryStorage();
        _telemetryStorageConsumer = telemetryStorage;
        _telemetryStorageProducer = telemetryStorage;

        if (config.blockUntilReady() == -1) {
            //BlockUntilReady not been set
            _log.warn("no setBlockUntilReadyTimeout parameter has been set - incorrect control treatments could be logged” " +
                    "if no ready config has been set when building factory");

        }

        // SDKReadinessGates
        _gates = new SDKReadinessGates();

        // HttpClient
        _httpclient = buildHttpClient(apiToken, config, _sdkMetadata);

        // Roots
        _rootTarget = URI.create(config.endpoint());
        _eventsRootTarget = URI.create(config.eventsEndpoint());

        // Cache Initialisations
        _segmentCache = new SegmentCacheInMemoryImpl();
        _splitCache = new InMemoryCacheImp();
        _telemetrySynchronizer = new TelemetryInMemorySubmitter(_httpclient, URI.create(config.telemetryURL()), _telemetryStorage, _splitCache, _segmentCache, _telemetryStorage, _startTime);


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

        // EventClient
        _eventsStorage = new InMemoryEventsStorage(config.eventsQueueSize(), _telemetryStorageProducer);
        _eventsTask = EventsTask.create(_httpclient,
                _eventsRootTarget,
                config.eventsQueueSize(),
                config.eventFlushIntervalInMillis(),
                config.waitBeforeShutdown(),
                _telemetryStorageProducer, _eventsStorage);

        _telemetrySyncTask = new TelemetrySyncTask(config.get_telemetryRefreshRate(), _telemetrySynchronizer);

        // Evaluator
        _evaluator = new EvaluatorImp(_splitCache, _segmentCache);

        // SplitClient
        _client = new SplitClientImpl(this,
                _splitCache,
                _impressionsManager,
                _eventsStorage,
                config,
                _gates,
                _evaluator,
                _telemetryStorageProducer,
                _telemetryStorageProducer);

        // SplitManager
        _manager = new SplitManagerImpl(_splitCache, config, _gates, _telemetryStorageProducer);

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
                buildSSEdHttpClient(apiToken, config, _sdkMetadata),
                _segmentCache,
                config.streamingRetryDelay(),
                config.streamingFetchMaxRetries(),
                config.failedAttemptsBeforeLogging(),
                config.cdnDebugLogging(), _gates, _telemetryStorageProducer, _telemetrySynchronizer,config);
        _syncManager.start();

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


    //Constructor for consumer mode
    public SplitFactoryImpl(String apiToken, SplitClientConfig config, CustomStorageWrapper customStorageWrapper) {
        _startTime = System.currentTimeMillis();
        _apiToken = apiToken;
        _apiKeyCounter = ApiKeyCounter.getApiKeyCounterInstance();
        _apiKeyCounter.add(apiToken);
        _telemetryStorageProducer = new UserCustomTelemetryAdapterProducer(customStorageWrapper, SplitClientConfig.splitSdkVersion, config.ipAddressEnabled());
        _telemetryStorageConsumer = new NoopTelemetryStorage();


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

        // Cache Initialisations
        _segmentCache = new SegmentCacheInMemoryImpl();
        _splitCache = new InMemoryCacheImp();

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
                long splitCount = _splitCache.getAll().stream().count();
                long segmentCount = _segmentCache.getSegmentCount();
                long segmentKeyCount = _segmentCache.getKeyCount();
                _impressionsManager.close();
                _log.info("Successful shutdown of impressions manager");
                _eventsTask.close();
                _log.info("Successful shutdown of eventsTask");
                _segmentSynchronizationTaskImp.close();
                _log.info("Successful shutdown of segment fetchers");
                _splitSynchronizationTask.close();
                _log.info("Successful shutdown of splits");
                _syncManager.shutdown();
                _log.info("Successful shutdown of syncManager");
                _telemetryStorageProducer.recordSessionLength(System.currentTimeMillis() - _startTime);
                _telemetrySyncTask.stopScheduledTask(splitCount, segmentCount, segmentKeyCount);
                _log.info("Successful shutdown of telemetry sync task");
                _httpclient.close();
                _log.info("Successful shutdown of httpclient");
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

    private static CloseableHttpClient buildHttpClient(String apiToken, SplitClientConfig config, SDKMetadata sdkMetadata) {
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
                .setValidateAfterInactivity(TimeValue.ofMilliseconds(config.validateAfterInactivityInMillis()))
                .build();
        cm.setMaxTotal(20);
        cm.setDefaultMaxPerRoute(20);

        HttpClientBuilder httpClientbuilder = HttpClients.custom()
                .setConnectionManager(cm)
                .setDefaultRequestConfig(requestConfig)
                .addRequestInterceptorLast(AuthorizationInterceptorFilter.instance(apiToken))
                .addRequestInterceptorLast(SdkMetadataInterceptorFilter.instance(sdkMetadata))
                .addRequestInterceptorLast(new GzipEncoderRequestInterceptor())
                .addResponseInterceptorLast((new GzipDecoderResponseInterceptor()));

        // Set up proxy is it exists
        if (config.proxy() != null) {
            httpClientbuilder = setupProxy(httpClientbuilder, config);
        }

        return httpClientbuilder.build();
    }

    private static CloseableHttpClient buildSSEdHttpClient(String apiToken, SplitClientConfig config, SDKMetadata sdkMetadata) {
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
                .setDefaultRequestConfig(requestConfig)
                .addRequestInterceptorLast(SdkMetadataInterceptorFilter.instance(sdkMetadata))
                .addRequestInterceptorLast(ClientKeyInterceptorFilter.instance(apiToken));

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
        SegmentChangeFetcher segmentChangeFetcher = HttpSegmentChangeFetcher.create(_httpclient, _rootTarget, _telemetryStorageProducer);

        return new SegmentSynchronizationTaskImp(segmentChangeFetcher,
                findPollingPeriod(RANDOM, config.segmentsRefreshRate()),
                config.numThreadsForSegmentFetch(),
                _gates,
                _segmentCache,
                _telemetryStorageProducer);
    }

    private SplitFetcher buildSplitFetcher() throws URISyntaxException {
        SplitChangeFetcher splitChangeFetcher = HttpSplitChangeFetcher.create(_httpclient, _rootTarget, _telemetryStorageProducer);
        SplitParser splitParser = new SplitParser();

        return new SplitFetcherImp(splitChangeFetcher, splitParser, _splitCache, _splitCache, _telemetryStorageProducer);
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

        return ImpressionsManagerImpl.instance(_httpclient, config, impressionListeners, _telemetryStorageProducer);
    }

    private SDKMetadata createSdkMetadata(boolean ipAddressEnabled, String splitSdkVersion) {
        String machineName = "";
        String ip = "";

        if (ipAddressEnabled) {
            try {
                InetAddress localHost = InetAddress.getLocalHost();
                machineName = localHost.getHostName();
                ip = localHost.getHostAddress();
            } catch (Exception e) {
                _log.error("Could not resolve InetAddress", e);
            }
        }
        return new SDKMetadata(splitSdkVersion, ip, machineName);
    }
}
