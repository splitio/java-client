package io.split.client;

import com.google.common.io.Files;
import io.split.client.dtos.Metadata;
import io.split.client.events.EventsSender;
import io.split.client.events.EventsStorage;
import io.split.client.events.EventsTask;
import io.split.client.events.InMemoryEventsStorage;
import io.split.client.events.NoopEventsStorageImp;
import io.split.client.impressions.AsynchronousImpressionListener;
import io.split.client.impressions.HttpImpressionsSender;
import io.split.client.impressions.ImpressionCounter;
import io.split.client.impressions.ImpressionListener;
import io.split.client.impressions.ImpressionObserver;
import io.split.client.impressions.ImpressionsManager;
import io.split.client.impressions.ImpressionsManagerImpl;
import io.split.client.impressions.ImpressionsSender;
import io.split.client.impressions.ImpressionsStorage;
import io.split.client.impressions.ImpressionsStorageConsumer;
import io.split.client.impressions.ImpressionsStorageProducer;
import io.split.client.impressions.InMemoryImpressionsStorage;
import io.split.client.impressions.PluggableImpressionSender;
import io.split.client.impressions.UniqueKeysTracker;
import io.split.client.impressions.UniqueKeysTrackerImp;
import io.split.client.impressions.strategy.ProcessImpressionDebug;
import io.split.client.impressions.strategy.ProcessImpressionNone;
import io.split.client.impressions.strategy.ProcessImpressionOptimized;
import io.split.client.impressions.strategy.ProcessImpressionStrategy;
import io.split.client.interceptors.ClientKeyInterceptorFilter;
import io.split.client.interceptors.FlagSetsFilter;
import io.split.client.interceptors.FlagSetsFilterImpl;
import io.split.client.interceptors.GzipDecoderResponseInterceptor;
import io.split.client.interceptors.GzipEncoderRequestInterceptor;
import io.split.client.interceptors.SdkMetadataInterceptorFilter;
import io.split.client.utils.FileInputStreamProvider;
import io.split.client.utils.FileTypeEnum;
import io.split.client.utils.InputStreamProvider;
import io.split.client.utils.SDKMetadata;
import io.split.client.utils.StaticContentInputStreamProvider;
import io.split.engine.SDKReadinessGates;
import io.split.engine.common.ConsumerSyncManager;
import io.split.engine.common.ConsumerSynchronizer;
import io.split.engine.common.LocalhostSyncManager;
import io.split.engine.common.LocalhostSynchronizer;
import io.split.engine.common.SplitAPI;
import io.split.engine.common.SplitTasks;
import io.split.engine.common.SyncManager;
import io.split.engine.common.SyncManagerImp;
import io.split.engine.common.Synchronizer;
import io.split.engine.evaluator.Evaluator;
import io.split.engine.evaluator.EvaluatorImp;
import io.split.engine.experiments.SplitChangeFetcher;
import io.split.engine.experiments.SplitFetcher;
import io.split.engine.experiments.SplitFetcherImp;
import io.split.engine.experiments.SplitParser;
import io.split.engine.experiments.RuleBasedSegmentParser;
import io.split.engine.experiments.SplitSynchronizationTask;
import io.split.engine.segments.SegmentChangeFetcher;
import io.split.engine.segments.SegmentSynchronizationTaskImp;
import io.split.integrations.IntegrationsConfig;
import io.split.service.SplitHttpClientImpl;
import io.split.service.SplitHttpClient;

import io.split.storages.SegmentCache;
import io.split.storages.SegmentCacheConsumer;
import io.split.storages.SegmentCacheProducer;
import io.split.storages.SplitCache;
import io.split.storages.SplitCacheConsumer;
import io.split.storages.SplitCacheProducer;
import io.split.storages.RuleBasedSegmentCacheConsumer;
import io.split.storages.RuleBasedSegmentCache;
import io.split.storages.enums.OperationMode;
import io.split.storages.memory.InMemoryCacheImp;
import io.split.storages.memory.RuleBasedSegmentCacheInMemoryImp;
import io.split.storages.memory.SegmentCacheInMemoryImpl;
import io.split.storages.pluggable.adapters.UserCustomEventAdapterProducer;
import io.split.storages.pluggable.adapters.UserCustomImpressionAdapterConsumer;
import io.split.storages.pluggable.adapters.UserCustomImpressionAdapterProducer;
import io.split.storages.pluggable.adapters.UserCustomSegmentAdapterConsumer;
import io.split.storages.pluggable.adapters.UserCustomSplitAdapterConsumer;
import io.split.storages.pluggable.adapters.UserCustomTelemetryAdapterProducer;
import io.split.storages.pluggable.domain.UserStorageWrapper;
import io.split.storages.pluggable.synchronizer.TelemetryConsumerSubmitter;
import io.split.telemetry.storage.InMemoryTelemetryStorage;
import io.split.telemetry.storage.NoopTelemetryStorage;
import io.split.telemetry.storage.TelemetryStorage;
import io.split.telemetry.storage.TelemetryStorageProducer;
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
import org.slf4j.LoggerFactory;
import pluggable.CustomStorageWrapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;

import static io.split.client.utils.SplitExecutorFactory.buildExecutorService;

public class SplitFactoryImpl implements SplitFactory {
    private static final org.slf4j.Logger _log = LoggerFactory.getLogger(SplitFactoryImpl.class);
    private static final String LEGACY_LOG_MESSAGE = "The sdk initialize in localhost mode using Legacy file. The splitFile or "
            + "inputStream were not added to the config.";
    private final static long SSE_CONNECT_TIMEOUT = 30000;
    private final static long SSE_SOCKET_TIMEOUT = 70000;

    private final SDKReadinessGates _gates;
    private final ImpressionsManager _impressionsManager;
    private final Evaluator _evaluator;
    private final String _apiToken;

    // Client and Manager
    private final SplitClient _client;
    private final SplitManager _manager;

    // Cache
    private final SplitCacheConsumer _splitCache;
    private final SegmentCacheConsumer _segmentCache;

    private boolean isTerminated = false;
    private final ApiKeyCounter _apiKeyCounter;
    private final TelemetryStorageProducer _telemetryStorageProducer;
    private final TelemetrySynchronizer _telemetrySynchronizer;
    private long _startTime;
    private final SDKMetadata _sdkMetadata;
    private OperationMode _operationMode;

    // Depending on mode are not mandatory
    private final TelemetrySyncTask _telemetrySyncTask;
    private final SegmentSynchronizationTaskImp _segmentSynchronizationTaskImp;
    private final SplitFetcher _splitFetcher;
    private final SplitSynchronizationTask _splitSynchronizationTask;
    private final EventsTask _eventsTask;
    private final SyncManager _syncManager;
    private SplitHttpClient _splitHttpClient;
    private final UserStorageWrapper _userStorageWrapper;
    private final ImpressionsSender _impressionsSender;
    private final URI _rootTarget;
    private final URI _eventsRootTarget;
    private final UniqueKeysTracker _uniqueKeysTracker;
    private RequestDecorator _requestDecorator;

    // Constructor for standalone mode
    public SplitFactoryImpl(String apiToken, SplitClientConfig config) throws URISyntaxException, IOException {
        _userStorageWrapper = null;
        _operationMode = config.operationMode();
        _startTime = System.currentTimeMillis();
        _apiToken = apiToken;
        _apiKeyCounter = ApiKeyCounter.getApiKeyCounterInstance();
        _apiKeyCounter.add(apiToken);
        _sdkMetadata = createSdkMetadata(config.ipAddressEnabled(), SplitClientConfig.splitSdkVersion);

        TelemetryStorage telemetryStorage = new InMemoryTelemetryStorage();
        _telemetryStorageProducer = telemetryStorage;

        if (config.blockUntilReady() == -1) {
            // BlockUntilReady not been set
            _log.warn(
                    "no setBlockUntilReadyTimeout parameter has been set - incorrect control treatments could be logged” "
                            +
                            "if no ready config has been set when building factory");
        }

        // SDKReadinessGates
        _gates = new SDKReadinessGates();

        _requestDecorator = new RequestDecorator(config.customHeaderDecorator());
        // HttpClient
        if (config.alternativeHTTPModule() == null) {
            _splitHttpClient = buildSplitHttpClient(apiToken, config, _sdkMetadata, _requestDecorator);
        } else {
            _splitHttpClient = config.alternativeHTTPModule().createClient(apiToken, _sdkMetadata, _requestDecorator);
        }

        // Roots
        _rootTarget = URI.create(config.endpoint());
        _eventsRootTarget = URI.create(config.eventsEndpoint());

        // Cache Initialisations
        SegmentCache segmentCache = new SegmentCacheInMemoryImpl();
        FlagSetsFilter flagSetsFilter = new FlagSetsFilterImpl(config.getSetsFilter());
        SplitCache splitCache = new InMemoryCacheImp(flagSetsFilter);
        RuleBasedSegmentCache ruleBasedSegmentCache = new RuleBasedSegmentCacheInMemoryImp();
        ImpressionsStorage impressionsStorage = new InMemoryImpressionsStorage(config.impressionsQueueSize());
        _splitCache = splitCache;
        _segmentCache = segmentCache;
        _telemetrySynchronizer = new TelemetryInMemorySubmitter(_splitHttpClient, URI.create(config.telemetryURL()),
                telemetryStorage,
                splitCache, _segmentCache, telemetryStorage, _startTime);

        // Segments
        _segmentSynchronizationTaskImp = buildSegments(config, segmentCache, splitCache);

        SplitParser splitParser = new SplitParser();
        RuleBasedSegmentParser ruleBasedSegmentParser = new RuleBasedSegmentParser();

        // SplitFetcher
        _splitFetcher = buildSplitFetcher(splitCache, splitParser, flagSetsFilter, ruleBasedSegmentParser, ruleBasedSegmentCache);


        // SplitSynchronizationTask
        _splitSynchronizationTask = new SplitSynchronizationTask(_splitFetcher,
                splitCache,
                config.featuresRefreshRate(),
                config.getThreadFactory());

        // ImpressionSender
        _impressionsSender = HttpImpressionsSender.create(_splitHttpClient, URI.create(config.eventsEndpoint()),
                config.impressionsMode(),
                _telemetryStorageProducer);

        // UniqueKeysTracker
        _uniqueKeysTracker = createUniqueKeysTracker(config);

        // Impressions
        _impressionsManager = buildImpressionsManager(config, impressionsStorage, impressionsStorage);

        // EventClient
        EventsStorage eventsStorage = new InMemoryEventsStorage(config.eventsQueueSize(), _telemetryStorageProducer);
        EventsSender eventsSender = EventsSender.create(_splitHttpClient, _eventsRootTarget, _telemetryStorageProducer);
        _eventsTask = EventsTask.create(config.eventSendIntervalInMillis(), eventsStorage, eventsSender,
                config.getThreadFactory());
        _telemetrySyncTask = new TelemetrySyncTask(config.getTelemetryRefreshRate(), _telemetrySynchronizer,
                config.getThreadFactory());

        // Evaluator
        _evaluator = new EvaluatorImp(splitCache, segmentCache, ruleBasedSegmentCache);

        // SplitClient
        _client = new SplitClientImpl(this,
                splitCache,
                _impressionsManager,
                eventsStorage,
                config,
                _gates,
                _evaluator,
                _telemetryStorageProducer, // TelemetryEvaluation instance
                _telemetryStorageProducer, // TelemetryConfiguration instance
                flagSetsFilter);

        // SplitManager
        _manager = new SplitManagerImpl(splitCache, config, _gates, _telemetryStorageProducer);

        // SyncManager
        SplitTasks splitTasks = SplitTasks.build(_splitSynchronizationTask, _segmentSynchronizationTaskImp,
                _impressionsManager, _eventsTask, _telemetrySyncTask, _uniqueKeysTracker);
        SplitAPI splitAPI = SplitAPI.build(_splitHttpClient, buildSSEdHttpClient(apiToken, config, _sdkMetadata),
                _requestDecorator);

        _syncManager = SyncManagerImp.build(splitTasks, _splitFetcher, splitCache, splitAPI,
                segmentCache, _gates, _telemetryStorageProducer, _telemetrySynchronizer, config, splitParser,
                flagSetsFilter);
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

    // Constructor for consumer mode
    protected SplitFactoryImpl(String apiToken, SplitClientConfig config, CustomStorageWrapper customStorageWrapper)
            throws URISyntaxException {
        // Variables that are not used in Consumer mode.
        _segmentSynchronizationTaskImp = null;
        _splitFetcher = null;
        _splitSynchronizationTask = null;
        _eventsTask = null;
        _splitHttpClient = null;
        _rootTarget = null;
        _eventsRootTarget = null;

        Metadata metadata = new Metadata(config.ipAddressEnabled(), SplitClientConfig.splitSdkVersion);
        _userStorageWrapper = new UserStorageWrapper(customStorageWrapper);
        UserCustomSegmentAdapterConsumer userCustomSegmentAdapterConsumer = new UserCustomSegmentAdapterConsumer(
                customStorageWrapper);
        UserCustomSplitAdapterConsumer userCustomSplitAdapterConsumer = new UserCustomSplitAdapterConsumer(
                customStorageWrapper);
        // TODO Update the instance to UserCustomRuleBasedSegmentAdapterConsumer
        RuleBasedSegmentCacheConsumer userCustomRuleBasedSegmentAdapterConsumer = new RuleBasedSegmentCacheInMemoryImp();

        // TODO migrate impressions sender to Task instead manager and not instantiate
        // Producer here.
        UserCustomImpressionAdapterConsumer userCustomImpressionAdapterConsumer = new UserCustomImpressionAdapterConsumer();
        UserCustomImpressionAdapterProducer userCustomImpressionAdapterProducer = new UserCustomImpressionAdapterProducer(
                customStorageWrapper,
                metadata);
        UserCustomEventAdapterProducer userCustomEventAdapterProducer = new UserCustomEventAdapterProducer(
                customStorageWrapper, metadata);

        _operationMode = config.operationMode();
        _sdkMetadata = createSdkMetadata(config.ipAddressEnabled(), SplitClientConfig.splitSdkVersion);
        _startTime = System.currentTimeMillis();
        _apiToken = apiToken;
        _apiKeyCounter = ApiKeyCounter.getApiKeyCounterInstance();
        _apiKeyCounter.add(apiToken);
        _telemetryStorageProducer = new UserCustomTelemetryAdapterProducer(customStorageWrapper, _sdkMetadata);

        _splitCache = userCustomSplitAdapterConsumer;
        _segmentCache = userCustomSegmentAdapterConsumer;

        if (config.blockUntilReady() == -1) {
            // BlockUntilReady not been set
            _log.warn(
                    "no setBlockUntilReadyTimeout parameter has been set - incorrect control treatments could be logged” "
                            +
                            "if no ready config has been set when building factory");
        }

        // SDKReadinessGates
        _gates = new SDKReadinessGates();

        _telemetrySynchronizer = new TelemetryConsumerSubmitter(customStorageWrapper, _sdkMetadata);
        _evaluator = new EvaluatorImp(userCustomSplitAdapterConsumer, userCustomSegmentAdapterConsumer, userCustomRuleBasedSegmentAdapterConsumer);
        _impressionsSender = PluggableImpressionSender.create(customStorageWrapper);
        _uniqueKeysTracker = createUniqueKeysTracker(config);
        _impressionsManager = buildImpressionsManager(config, userCustomImpressionAdapterConsumer,
                userCustomImpressionAdapterProducer);
        _telemetrySyncTask = new TelemetrySyncTask(config.getTelemetryRefreshRate(), _telemetrySynchronizer,
                config.getThreadFactory());

        SplitTasks splitTasks = SplitTasks.build(null, null,
                _impressionsManager, null, _telemetrySyncTask, _uniqueKeysTracker);

        // Synchronizer
        Synchronizer synchronizer = new ConsumerSynchronizer(splitTasks);
        FlagSetsFilter flagSetsFilter = new FlagSetsFilterImpl(new HashSet<>());
        if (!config.getSetsFilter().isEmpty()) {
            _log.warn(
                    "FlagSets filter is not applicable for Consumer modes where the SDK does not keep rollout data in sync. FlagSet "
                            +
                            "filter was discarded");
        }
        _client = new SplitClientImpl(this,
                userCustomSplitAdapterConsumer,
                _impressionsManager,
                userCustomEventAdapterProducer,
                config,
                _gates,
                _evaluator,
                _telemetryStorageProducer, // TelemetryEvaluation instance
                _telemetryStorageProducer, // TelemetryConfiguration instance
                flagSetsFilter);

        // SyncManager
        _syncManager = new ConsumerSyncManager(synchronizer);
        _syncManager.start();

        _manager = new SplitManagerImpl(userCustomSplitAdapterConsumer, config, _gates, _telemetryStorageProducer);
        manageSdkReady(config);
    }

    // Localhost
    protected SplitFactoryImpl(SplitClientConfig config) {
        _userStorageWrapper = null;
        _apiToken = "localhost";
        _apiKeyCounter = ApiKeyCounter.getApiKeyCounterInstance();
        _apiKeyCounter.add("localhost");
        _sdkMetadata = createSdkMetadata(config.ipAddressEnabled(), SplitClientConfig.splitSdkVersion);
        _telemetrySynchronizer = null;
        _telemetrySyncTask = null;
        _eventsTask = null;
        _splitHttpClient = null;
        _impressionsSender = null;
        _rootTarget = null;
        _eventsRootTarget = null;
        _uniqueKeysTracker = null;
        _telemetryStorageProducer = new NoopTelemetryStorage();

        SegmentCache segmentCache = new SegmentCacheInMemoryImpl();
        FlagSetsFilter flagSetsFilter = new FlagSetsFilterImpl(config.getSetsFilter());
        RuleBasedSegmentCache ruleBasedSegmentCache = new RuleBasedSegmentCacheInMemoryImp();
        SplitCache splitCache = new InMemoryCacheImp(flagSetsFilter);
        _splitCache = splitCache;
        _gates = new SDKReadinessGates();
        _segmentCache = segmentCache;

        // SegmentFetcher

        SegmentChangeFetcher segmentChangeFetcher = new LocalhostSegmentFetcherNoop();
        if (config.segmentDirectory() != null) {
            segmentChangeFetcher = new LocalhostSegmentChangeFetcher(config.segmentDirectory());
        }

        _segmentSynchronizationTaskImp = new SegmentSynchronizationTaskImp(segmentChangeFetcher,
                config.segmentsRefreshRate(),
                config.numThreadsForSegmentFetch(),
                segmentCache,
                _telemetryStorageProducer,
                _splitCache,
                config.getThreadFactory());

        // SplitFetcher
        SplitChangeFetcher splitChangeFetcher = createSplitChangeFetcher(config);
        RuleBasedSegmentParser ruleBasedSegmentParser = new RuleBasedSegmentParser();
        SplitParser splitParser = new SplitParser();

        _splitFetcher = new SplitFetcherImp(splitChangeFetcher, splitParser, splitCache, _telemetryStorageProducer,
                flagSetsFilter, ruleBasedSegmentParser, ruleBasedSegmentCache);

        // SplitSynchronizationTask
        _splitSynchronizationTask = new SplitSynchronizationTask(_splitFetcher, splitCache,
                config.featuresRefreshRate(), config.getThreadFactory());

        _impressionsManager = new ImpressionsManager.NoOpImpressionsManager();

        SplitTasks splitTasks = SplitTasks.build(_splitSynchronizationTask, _segmentSynchronizationTaskImp,
                _impressionsManager, null, null, null);

        // Evaluator
        _evaluator = new EvaluatorImp(splitCache, segmentCache, ruleBasedSegmentCache);

        EventsStorage eventsStorage = new NoopEventsStorageImp();

        // SplitClient
        _client = new SplitClientImpl(this,
                splitCache,
                _impressionsManager,
                eventsStorage,
                config,
                _gates,
                _evaluator,
                _telemetryStorageProducer, // TelemetryEvaluation instance
                _telemetryStorageProducer, // TelemetryConfiguration instance
                flagSetsFilter);

        // Synchronizer
        Synchronizer synchronizer = new LocalhostSynchronizer(splitTasks, _splitFetcher,
                config.localhostRefreshEnabled());

        // SplitManager
        _manager = new SplitManagerImpl(splitCache, config, _gates, _telemetryStorageProducer);
        // SyncManager
        _syncManager = new LocalhostSyncManager(synchronizer, _gates);
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
        if (isTerminated) {
            return;
        }
        try {
            _log.info("Shutdown called for split");
            _syncManager.shutdown();
            _log.info("Successful shutdown of syncManager");
            if (OperationMode.STANDALONE.equals(_operationMode)) {
                _telemetryStorageProducer.recordSessionLength(System.currentTimeMillis() - _startTime);
            } else if (OperationMode.CONSUMER.equals(_operationMode)) {
                _userStorageWrapper.disconnect();
            }
        } catch (IOException e) {
            _log.error("We could not shutdown split", e);
        }
        _apiKeyCounter.remove(_apiToken);
        isTerminated = true;
    }

    @Override
    public boolean isDestroyed() {
        return isTerminated;
    }

    protected static SplitHttpClient buildSplitHttpClient(String apiToken, SplitClientConfig config,
            SDKMetadata sdkMetadata, RequestDecorator requestDecorator)
            throws URISyntaxException {
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
                .addRequestInterceptorLast(new GzipEncoderRequestInterceptor())
                .addResponseInterceptorLast((new GzipDecoderResponseInterceptor()));

        // Set up proxy is it exists
        if (config.proxy() != null) {
            httpClientbuilder = setupProxy(httpClientbuilder, config);
        }

        return SplitHttpClientImpl.create(httpClientbuilder.build(),
                requestDecorator,
                apiToken,
                sdkMetadata);
    }

    private static CloseableHttpClient buildSSEdHttpClient(String apiToken, SplitClientConfig config,
            SDKMetadata sdkMetadata) {
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
            Credentials siteCreds = new UsernamePasswordCredentials(config.proxyUsername(),
                    config.proxyPassword().toCharArray());
            credsProvider.setCredentials(siteScope, siteCreds);
            httpClientbuilder.setDefaultCredentialsProvider(credsProvider);
        }

        return httpClientbuilder;
    }

    private SegmentSynchronizationTaskImp buildSegments(SplitClientConfig config,
            SegmentCacheProducer segmentCacheProducer,
            SplitCacheConsumer splitCacheConsumer) throws URISyntaxException {
        SegmentChangeFetcher segmentChangeFetcher = HttpSegmentChangeFetcher.create(_splitHttpClient, _rootTarget,
                _telemetryStorageProducer);

        return new SegmentSynchronizationTaskImp(segmentChangeFetcher,
                config.segmentsRefreshRate(),
                config.numThreadsForSegmentFetch(),
                segmentCacheProducer,
                _telemetryStorageProducer,
                splitCacheConsumer,
                config.getThreadFactory());
    }

    private SplitFetcher buildSplitFetcher(SplitCacheProducer splitCacheProducer, SplitParser splitParser,
                                           FlagSetsFilter flagSetsFilter, RuleBasedSegmentParser ruleBasedSegmentParser,
                                           RuleBasedSegmentCache ruleBasedSegmentCache) throws URISyntaxException {
        SplitChangeFetcher splitChangeFetcher = HttpSplitChangeFetcher.create(_splitHttpClient, _rootTarget,
                _telemetryStorageProducer);
        return new SplitFetcherImp(splitChangeFetcher, splitParser, splitCacheProducer, _telemetryStorageProducer,
                flagSetsFilter,ruleBasedSegmentParser, ruleBasedSegmentCache);
    }

    private ImpressionsManagerImpl buildImpressionsManager(SplitClientConfig config,
            ImpressionsStorageConsumer impressionsStorageConsumer,
            ImpressionsStorageProducer impressionsStorageProducer) throws URISyntaxException {
        List<ImpressionListener> impressionListeners = new ArrayList<>();
        if (config.integrationsConfig() != null) {
            config.integrationsConfig().getImpressionsListeners(IntegrationsConfig.Execution.ASYNC).stream()
                    .map(l -> AsynchronousImpressionListener.build(l.listener(), l.queueSize()))
                    .collect(Collectors.toCollection(() -> impressionListeners));

            config.integrationsConfig().getImpressionsListeners(IntegrationsConfig.Execution.SYNC).stream()
                    .map(IntegrationsConfig.ImpressionListenerWithMeta::listener)
                    .collect(Collectors.toCollection(() -> impressionListeners));
        }
        ProcessImpressionStrategy processImpressionStrategy = null;
        ImpressionCounter counter = new ImpressionCounter();
        ImpressionListener listener = !impressionListeners.isEmpty()
                ? new ImpressionListener.FederatedImpressionListener(impressionListeners)
                : null;
        ProcessImpressionNone processImpressionNone = new ProcessImpressionNone(listener != null, _uniqueKeysTracker, counter);

        switch (config.impressionsMode()) {
            case OPTIMIZED:
                ImpressionObserver impressionObserver = new ImpressionObserver(config.getLastSeenCacheSize());
                processImpressionStrategy = new ProcessImpressionOptimized(listener != null, impressionObserver,
                        counter, _telemetryStorageProducer);
                break;
            case DEBUG:
                impressionObserver = new ImpressionObserver(config.getLastSeenCacheSize());
                processImpressionStrategy = new ProcessImpressionDebug(listener != null, impressionObserver);
                break;
            case NONE:
                processImpressionStrategy = processImpressionNone;
                break;
        }
        return ImpressionsManagerImpl.instance(config, _telemetryStorageProducer, impressionsStorageConsumer,
                impressionsStorageProducer,
                _impressionsSender, processImpressionNone, processImpressionStrategy, counter, listener);
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

    private void manageSdkReady(SplitClientConfig config) {
        ExecutorService executorService = buildExecutorService(config.getThreadFactory(),
                "SPLIT-SDKReadyForConsumer-%d");
        executorService.submit(() -> {
            while (!_userStorageWrapper.connect()) {
                try {
                    Thread.currentThread().sleep(1000);
                } catch (InterruptedException e) {
                    _log.warn("Sdk Initializer thread interrupted");
                    Thread.currentThread().interrupt();
                }
            }
            _gates.sdkInternalReady();
            _telemetrySynchronizer.synchronizeConfig(config, System.currentTimeMillis(),
                    ApiKeyCounter.getApiKeyCounterInstance().getFactoryInstances(), new ArrayList<>());
        });
    }

    private UniqueKeysTracker createUniqueKeysTracker(SplitClientConfig config) {
        int uniqueKeysRefreshRate = config.operationMode().equals(OperationMode.STANDALONE)
                ? config.uniqueKeysRefreshRateInMemory()
                : config.uniqueKeysRefreshRateRedis();
        return new UniqueKeysTrackerImp(_telemetrySynchronizer, uniqueKeysRefreshRate,
                config.filterUniqueKeysRefreshRate(),
                config.getThreadFactory());
    }

    private SplitChangeFetcher createSplitChangeFetcher(SplitClientConfig splitClientConfig) {
        String splitFile = splitClientConfig.splitFile();
        InputStream inputStream = splitClientConfig.inputStream();
        FileTypeEnum fileType = splitClientConfig.fileType();
        InputStreamProvider inputStreamProvider;
        if (splitFile != null || !isInputStreamConfigValid(inputStream, fileType)) {
            if (splitFile == null) {
                _log.warn("The InputStream config is invalid");
            }
            fileType = getFileTypeFromFileName(splitFile);
            inputStreamProvider = new FileInputStreamProvider(splitFile);
        } else {
            inputStreamProvider = new StaticContentInputStreamProvider(inputStream);
        }
        try {
            switch (fileType) {
                case JSON:
                    return new JsonLocalhostSplitChangeFetcher(inputStreamProvider);
                case YAML:
                case YML:
                    return new YamlLocalhostSplitChangeFetcher(inputStreamProvider);
                default:
                    _log.warn(LEGACY_LOG_MESSAGE);
                    return new LegacyLocalhostSplitChangeFetcher(splitFile);
            }
        } catch (Exception e) {
            _log.warn(String.format("There was no file named %s found. " +
                    "We created a split client that returns default treatments for all feature flags for all of your users. "
                    +
                    "If you wish to return a specific treatment for a feature flag, enter the name of that feature flag name and "
                    +
                    "treatment name separated by whitespace in %s; one pair per line. Empty lines or lines starting with '#' are "
                    +
                    "considered comments",
                    splitFile, splitFile), e);
        }
        _log.warn(LEGACY_LOG_MESSAGE);
        return new LegacyLocalhostSplitChangeFetcher(splitFile);
    }

    private Boolean isInputStreamConfigValid(InputStream inputStream, FileTypeEnum fileType) {
        return inputStream != null && fileType != null;
    }

    private FileTypeEnum getFileTypeFromFileName(String fileName) {
        try {
            return FileTypeEnum.valueOf(Files.getFileExtension(fileName).toUpperCase());
        } catch (Exception e) {
            return FileTypeEnum.LEGACY;
        }

    }
}
