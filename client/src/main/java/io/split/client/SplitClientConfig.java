package io.split.client;

import io.split.client.impressions.ImpressionListener;
import io.split.client.impressions.ImpressionsManager;
import io.split.client.utils.FileTypeEnum;
import io.split.integrations.IntegrationsConfig;
import io.split.storages.enums.OperationMode;
import io.split.storages.enums.StorageMode;
import io.split.service.HttpAuthScheme;
import org.apache.hc.core5.http.HttpHost;
import pluggable.CustomStorageWrapper;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ThreadFactory;
import java.io.InputStream;

import static io.split.inputValidation.FlagSetsValidator.cleanup;

/**
 * Configurations for the SplitClient.
 *
 * @author adil
 */
public class SplitClientConfig {

    public static final String LOCALHOST_DEFAULT_FILE = "split.yaml";
    public static final String SDK_ENDPOINT = "https://sdk.split.io";
    public static final String EVENTS_ENDPOINT = "https://events.split.io";
    public static final String AUTH_ENDPOINT = "https://auth.split.io/api/v2/auth";
    public static final String STREAMING_ENDPOINT = "https://streaming.split.io/sse";
    public static final String TELEMETRY_ENDPOINT = "https://telemetry.split.io/api/v1";

    private final String _endpoint;
    private final String _eventsEndpoint;

    private final int _featuresRefreshRate;
    private final int _segmentsRefreshRate;
    private final int _impressionsRefreshRate;
    private final int _impressionsQueueSize;
    private final ImpressionsManager.Mode _impressionsMode;
    private final int _metricsRefreshRate;
    private final int _connectionTimeout;
    private final int _readTimeout;
    private final int _numThreadsForSegmentFetch;
    private final boolean _debugEnabled;
    private final boolean _labelsEnabled;
    private final boolean _ipAddressEnabled;
    private final boolean _localhostRefreshEnabled;
    private final int _ready;
    private final int _waitBeforeShutdown;
    private final int _eventsQueueSize;
    private final long _eventSendIntervalInMillis;
    private final int _maxStringLength;
    private final boolean _destroyOnShutDown;
    private final String _splitFile;
    private final FileTypeEnum _fileType;
    private final InputStream _inputStream;
    private final String _segmentDirectory;
    private final IntegrationsConfig _integrationsConfig;
    private final boolean _streamingEnabled;
    private final int _authRetryBackoffBase;
    private final int _streamingReconnectBackoffBase;
    private final String _authServiceURL;
    private final String _streamingServiceURL;
    private final String _telemetryURL;
    private final int _telemetryRefreshRate;
    private final int _onDemandFetchRetryDelayMs;
    private final int _onDemandFetchMaxRetries;
    private final int _failedAttemptsBeforeLogging;
    private final int _uniqueKeysRefreshRateInMemory;
    private final int _uniqueKeysRefreshRateRedis;
    private static int _filterUniqueKeysRefreshRate;
    private final OperationMode _operationMode;
    private long _validateAfterInactivityInMillis;
    private final long _startingSyncCallBackoffBaseMs;
    private final CustomStorageWrapper _customStorageWrapper;
    private final StorageMode _storageMode;
    private final ThreadFactory _threadFactory;

    // Proxy configs
    private final HttpHost _proxy;
    private final String _proxyUsername;
    private final String _proxyPassword;

    // To be set during startup
    public static String splitSdkVersion;
    private final long _lastSeenCacheSize;
    private final HashSet<String> _flagSetsFilter;
    private final int _invalidSets;
    private final CustomHeaderDecorator _customHeaderDecorator;
    private final HttpAuthScheme _authScheme;
    private final String _kerberosPrincipalName;


    public static Builder builder() {
        return new Builder();
    }

    private SplitClientConfig(String endpoint,
                              String eventsEndpoint,
                              int pollForFeatureChangesEveryNSeconds,
                              int segmentsRefreshRate,
                              int impressionsRefreshRate,
                              int impressionsQueueSize,
                              ImpressionsManager.Mode impressionsMode,
                              int metricsRefreshRate,
                              int connectionTimeout,
                              int readTimeout,
                              int numThreadsForSegmentFetch,
                              int ready,
                              boolean debugEnabled,
                              boolean labelsEnabled,
                              boolean ipAddressEnabled,
                              boolean localhostRefreshEnabled,
                              int waitBeforeShutdown,
                              HttpHost proxy,
                              String proxyUsername,
                              String proxyPassword,
                              int eventsQueueSize,
                              long eventSendIntervalInMillis,
                              int maxStringLength,
                              boolean destroyOnShutDown,
                              String splitFile,
                              FileTypeEnum fileType,
                              InputStream inputStream,
                              String segmentDirectory,
                              IntegrationsConfig integrationsConfig,
                              boolean streamingEnabled,
                              int authRetryBackoffBase,
                              int streamingReconnectBackoffBase,
                              String authServiceURL,
                              String streamingServiceURL,
                              String telemetryURL,
                              int telemetryRefreshRate,
                              int onDemandFetchRetryDelayMs,
                              int onDemandFetchMaxRetries,
                              int failedAttemptsBeforeLogging,
                              OperationMode operationMode,
                              long validateAfterInactivityInMillis,
                              long startingSyncCallBackoffBaseMs,
                              CustomStorageWrapper customStorageWrapper,
                              StorageMode storageMode,
                              int uniqueKeysRefreshRateInMemory,
                              int uniqueKeysRefreshRateRedis,
                              int filterUniqueKeysRefreshRate,
                              long lastSeenCacheSize,
                              ThreadFactory threadFactory,
                              HashSet<String> flagSetsFilter,
                              int invalidSets,
                              CustomHeaderDecorator customHeaderDecorator,
                              HttpAuthScheme authScheme,
                              String kerberosPrincipalName) {
        _endpoint = endpoint;
        _eventsEndpoint = eventsEndpoint;
        _featuresRefreshRate = pollForFeatureChangesEveryNSeconds;
        _segmentsRefreshRate = segmentsRefreshRate;
        _impressionsRefreshRate = impressionsRefreshRate;
        _impressionsQueueSize = impressionsQueueSize;
        _impressionsMode = impressionsMode;
        _metricsRefreshRate = metricsRefreshRate;
        _connectionTimeout = connectionTimeout;
        _readTimeout = readTimeout;
        _numThreadsForSegmentFetch = numThreadsForSegmentFetch;
        _ready = ready;
        _debugEnabled = debugEnabled;
        _labelsEnabled = labelsEnabled;
        _ipAddressEnabled = ipAddressEnabled;
        _localhostRefreshEnabled = localhostRefreshEnabled;
        _waitBeforeShutdown = waitBeforeShutdown;
        _proxy = proxy;
        _proxyUsername = proxyUsername;
        _proxyPassword = proxyPassword;
        _eventsQueueSize = eventsQueueSize;
        _eventSendIntervalInMillis = eventSendIntervalInMillis;
        _maxStringLength = maxStringLength;
        _destroyOnShutDown = destroyOnShutDown;
        _splitFile = splitFile;
        _fileType = fileType;
        _inputStream = inputStream;
        _segmentDirectory = segmentDirectory;
        _integrationsConfig = integrationsConfig;
        _streamingEnabled = streamingEnabled;
        _authRetryBackoffBase = authRetryBackoffBase;
        _streamingReconnectBackoffBase = streamingReconnectBackoffBase;
        _authServiceURL = authServiceURL;
        _streamingServiceURL = streamingServiceURL;
        _telemetryURL = telemetryURL;
        _telemetryRefreshRate = telemetryRefreshRate;
        _uniqueKeysRefreshRateInMemory = uniqueKeysRefreshRateInMemory;
        _uniqueKeysRefreshRateRedis = uniqueKeysRefreshRateRedis;
        _filterUniqueKeysRefreshRate = filterUniqueKeysRefreshRate;
        _onDemandFetchRetryDelayMs = onDemandFetchRetryDelayMs;
        _onDemandFetchMaxRetries = onDemandFetchMaxRetries;
        _failedAttemptsBeforeLogging = failedAttemptsBeforeLogging;
        _operationMode = operationMode;
        _storageMode = storageMode;
        _validateAfterInactivityInMillis = validateAfterInactivityInMillis;
        _startingSyncCallBackoffBaseMs = startingSyncCallBackoffBaseMs;
        _customStorageWrapper = customStorageWrapper;
        _lastSeenCacheSize = lastSeenCacheSize;
        _threadFactory = threadFactory;
        _flagSetsFilter = flagSetsFilter;
        _invalidSets = invalidSets;
        _customHeaderDecorator = customHeaderDecorator;
        _authScheme = authScheme;
        _kerberosPrincipalName = kerberosPrincipalName;

        Properties props = new Properties();
        try {
            props.load(this.getClass().getClassLoader().getResourceAsStream("splitversion.properties"));
        } catch (IOException e) {
            throw new IllegalStateException("cannot find client version in classpath", e);
        }
        splitSdkVersion = "undefined";

        if (props.getProperty("sdk.version") != null) {
            splitSdkVersion = "java-" + props.getProperty("sdk.version");
        }
    }

    public String endpoint() {
        return _endpoint;
    }

    public String eventsEndpoint() {
        return _eventsEndpoint;
    }

    public int featuresRefreshRate() {
        return _featuresRefreshRate;
    }

    public int segmentsRefreshRate() {
        return _segmentsRefreshRate;
    }

    public int numThreadsForSegmentFetch() {
        return _numThreadsForSegmentFetch;
    }

    public int impressionsRefreshRate() {
        return _impressionsRefreshRate;
    }

    public int uniqueKeysRefreshRateInMemory() {
        return _uniqueKeysRefreshRateInMemory;
    }
    public int uniqueKeysRefreshRateRedis() {
        return _uniqueKeysRefreshRateRedis;
    }
    public static int filterUniqueKeysRefreshRate() {
        return _filterUniqueKeysRefreshRate;
    }

    public int impressionsQueueSize() {
        return _impressionsQueueSize;
    }

    public ImpressionsManager.Mode impressionsMode() { return _impressionsMode; }

    public int metricsRefreshRate() {
        return _metricsRefreshRate;
    }

    public int connectionTimeout() {
        return _connectionTimeout;
    }

    public int readTimeout() {
        return _readTimeout;
    }

    public boolean debugEnabled() {
        return _debugEnabled;
    }

    public boolean labelsEnabled() { return _labelsEnabled;}

    public boolean ipAddressEnabled() { return _ipAddressEnabled; }

    public boolean localhostRefreshEnabled() {
        return _localhostRefreshEnabled;
    }

    public int blockUntilReady() {
        return _ready;
    }

    public int waitBeforeShutdown() {
        return _waitBeforeShutdown;
    }

    public HttpHost proxy() {
        return _proxy;
    }

    public String proxyUsername() {
        return _proxyUsername;
    }

    public String proxyPassword() {
        return _proxyPassword;
    }

    public long eventSendIntervalInMillis() {
        return _eventSendIntervalInMillis;
    }

    public int eventsQueueSize() {
        return _eventsQueueSize;
    }

    public int maxStringLength() {
        return _maxStringLength;
    }

    public boolean destroyOnShutDown() {
        return _destroyOnShutDown;
    }

    public String splitFile() {
        return _splitFile;
    }

    public FileTypeEnum fileType() {
        return _fileType;
    }

    public InputStream inputStream(){
        return _inputStream;
    }

    public String segmentDirectory() {
        return _segmentDirectory;
    }

    public IntegrationsConfig integrationsConfig() {
        return _integrationsConfig;
    }

    public boolean streamingEnabled() {
        return _streamingEnabled;
    }

    public int authRetryBackoffBase() {
        return _authRetryBackoffBase;
    }

    public int streamingReconnectBackoffBase() {
        return _streamingReconnectBackoffBase;
    }

    public String authServiceURL() {
        return _authServiceURL;
    }

    public String streamingServiceURL() {
        return _streamingServiceURL;
    }

    public String telemetryURL() {
        return _telemetryURL;
    }

    /**
     * @deprecated  As of release 4.X.X, replaced by {@link #getTelemetryRefreshRate()} } //todo update version
     **/
    @Deprecated
    public int get_telemetryRefreshRate() {
        return _telemetryRefreshRate;
    }

    public int getTelemetryRefreshRate() {
        return _telemetryRefreshRate;
    }
    public int streamingRetryDelay() {return _onDemandFetchRetryDelayMs;}

    public int streamingFetchMaxRetries() {return _onDemandFetchMaxRetries;}

    public int failedAttemptsBeforeLogging() {return _failedAttemptsBeforeLogging;}

    public OperationMode operationMode() { return _operationMode;}

    public long validateAfterInactivityInMillis() {
        return _validateAfterInactivityInMillis;
    }
    public long startingSyncCallBackoffBaseMs(){ return  _startingSyncCallBackoffBaseMs;}

    public CustomStorageWrapper customStorageWrapper() {
        return _customStorageWrapper;
    }

    public StorageMode storageMode() { return _storageMode;}

    public long getLastSeenCacheSize() {
        return _lastSeenCacheSize;
    }

    public ThreadFactory getThreadFactory() {
        return _threadFactory;
    }

    public HashSet<String> getSetsFilter() {
        return _flagSetsFilter;
    }

    public int getInvalidSets() {
        return _invalidSets;
    }

    public CustomHeaderDecorator customHeaderDecorator() {
        return _customHeaderDecorator;
    }
    public HttpAuthScheme authScheme() {
        return _authScheme;
    }
    public String kerberosPrincipalName() { return _kerberosPrincipalName; }

    public static final class Builder {

        private String _endpoint = SDK_ENDPOINT;
        private boolean _endpointSet = false;
        private String _eventsEndpoint = EVENTS_ENDPOINT;
        private boolean _eventsEndpointSet = false;
        private int _featuresRefreshRate = 60;
        private int _segmentsRefreshRate = 60;
        private int _impressionsRefreshRate = -1; // use -1 to identify lack of a user submitted value & handle in build()
        private int _impressionsQueueSize = 30000;
        private ImpressionsManager.Mode _impressionsMode = ImpressionsManager.Mode.OPTIMIZED;
        private int _connectionTimeout = 15000;
        private int _readTimeout = 15000;
        private int _numThreadsForSegmentFetch = 10;
        private boolean _debugEnabled = false;
        private int _ready = -1; // -1 means no blocking
        private int _metricsRefreshRate = 60;
        private boolean _labelsEnabled = true;
        private boolean _ipAddressEnabled = true;
        private boolean _localhostRefreshEnable = false;
        private int _waitBeforeShutdown = 5000;
        private String _proxyHost = "localhost";
        private int _proxyPort = -1;
        private String _proxyUsername;
        private String _proxyPassword;
        private int _eventsQueueSize = 500;
        private long _eventSendIntervalInMillis = 30 * (long)1000;
        private int _maxStringLength = 250;
        private boolean _destroyOnShutDown = true;
        private String _splitFile = null;
        private FileTypeEnum _fileType = null;
        private InputStream _inputStream = null;
        private String _segmentDirectory = null;
        private IntegrationsConfig _integrationsConfig = null;
        private boolean _streamingEnabled = true;
        private int _authRetryBackoffBase = 1;
        private int _streamingReconnectBackoffBase = 1;
        private String _authServiceURL = AUTH_ENDPOINT;
        private String _streamingServiceURL = STREAMING_ENDPOINT;
        private String _telemetryURl = TELEMETRY_ENDPOINT;
        private int _telemetryRefreshRate = 600;
        private final int _uniqueKeysRefreshRateInMemory = 900;
        private final int _uniqueKeysRefreshRateRedis = 300;
        private final int _filterUniqueKeysRefreshRate = 86400;
        private int _onDemandFetchRetryDelayMs = 50;
        private final int _onDemandFetchMaxRetries = 10;
        private final int _failedAttemptsBeforeLogging = 10;
        private OperationMode _operationMode = OperationMode.STANDALONE;
        private long _validateAfterInactivityInMillis = 1000;
        private static final long STARTING_SYNC_CALL_BACKOFF_BASE_MS = 1000; //backoff base starting at 1 seconds
        private CustomStorageWrapper _customStorageWrapper;
        private StorageMode _storageMode = StorageMode.MEMORY;
        private final long _lastSeenCacheSize = 500000;
        private ThreadFactory _threadFactory;
        private HashSet<String> _flagSetsFilter = new HashSet<>();
        private int _invalidSetsCount = 0;
        private CustomHeaderDecorator _customHeaderDecorator = null;
        private HttpAuthScheme _authScheme = null;
        private String _kerberosPrincipalName = null;

        public Builder() {
        }

        /**
         * The amount of threads used for the thread pool that fetches segments.
         * Usually and for most cases 2 is more than enough. But for organization
         * that have a lot of segments, increasing this value can help expedite the
         * time to ready.
         * <p/>
         *
         * This is an ADVANCED parameter.
         *
         * @param numThreadsForSegmentFetch MUST be > 0. Default is 2.
         * @return this builder
         */
        public Builder numThreadsForSegmentFetch(int numThreadsForSegmentFetch) {
            _numThreadsForSegmentFetch = numThreadsForSegmentFetch;
            return this;
        }

        /**
         * Max size of the queue to trigger a flush
         *
         * @param eventsQueueSize
         * @return this builder
         */
        public Builder eventsQueueSize(int eventsQueueSize) {
            _eventsQueueSize = eventsQueueSize;
            return this;
        }

        /**
         * How often to flush data to the collection services
         *
         * @param eventFlushIntervalInMillis
         * @return this builder
         */
        public Builder eventFlushIntervalInMillis(long eventFlushIntervalInMillis) {
            _eventSendIntervalInMillis = eventFlushIntervalInMillis;
            return this;
        }

        /**
         * The rest endpoint that sdk will hit for latest features and segments.
         *
         * @param endpoint MUST NOT be null
         * @return this builder
         */
        public Builder endpoint(String endpoint, String eventsEndpoint) {
            _endpoint = endpoint;
            _eventsEndpoint = eventsEndpoint;
            return this;
        }

        /**
         * The SDK will poll the endpoint for changes to features at this period.
         * <p>
         * Implementation Note: The SDK actually polls at a random interval
         * chosen between (0.5 * n, n). This is to ensure that
         * SDKs that are deployed simultaneously on different machines do not
         * inundate the backend with requests at the same interval.
         * </p>
         *
         * @param seconds MUST be greater than 0. Default value is 60.
         * @return this builder
         */
        public Builder featuresRefreshRate(int seconds) {
            _featuresRefreshRate = seconds;
            return this;
        }

        /**
         * The SDK will poll the endpoint for changes to segments at this period in seconds.
         * <p>
         * Implementation Note: The SDK actually polls at a random interval
         * chosen between (0.5 * n, n). This is to ensure that
         * SDKs that are deployed simultaneously on different machines do not
         * inundate the backend with requests at the same interval.
         * </p>
         *
         * @param seconds MUST be greater than 0. Default value is 60.
         * @return this builder
         */
        public Builder segmentsRefreshRate(int seconds) {
            _segmentsRefreshRate = seconds;
            return this;
        }

        /**
         * The ImpressionListener captures the which key saw what treatment ("on", "off", etc)
         * at what time. This log is periodically pushed back to split endpoint.
         * This parameter controls how quickly does the cache expire after a write.
         * <p/>
         * This is an ADVANCED parameter
         *
         * @param seconds MUST be > 0.
         * @return this builder
         */
        public Builder impressionsRefreshRate(int seconds) {
            _impressionsRefreshRate = seconds;
            return this;
        }

        public Builder impressionsMode(ImpressionsManager.Mode mode) {
            _impressionsMode = mode;
            return this;
        }

        /**
         * The impression listener captures the which key saw what treatment ("on", "off", etc)
         * at what time. This log is periodically pushed back to split endpoint.
         * This parameter controls the in-memory queue size to store them before they are
         * pushed back to split endpoint.
         *
         * If the value chosen is too small and more than the default size(5000) of impressions
         * are generated, the old ones will be dropped and the sdk will show a warning.
         * <p>
         *
         * This is an ADVANCED parameter.
         *
         * @param impressionsQueueSize MUST be > 0. Default is 5000.
         * @return this builder
         */
        public Builder impressionsQueueSize(int impressionsQueueSize) {
            _impressionsQueueSize = impressionsQueueSize;
            return this;
        }

        /**
         *
         * @deprecated  As of release 3.2.5, replaced by {@link #integrationsConfig()} }
         *
         * You can provide your own ImpressionListener to capture all impressions
         * generated by SplitClient. An Impression is generated each time getTreatment is called.
         * <p>
         *
         * Note that we will wrap any ImpressionListener provided in our own implementation
         * with an Executor controlling impressions going into your ImpressionListener. This is
         * done to protect SplitClient from any slowness caused by your ImpressionListener. The
         * Executor will be given the capacity you provide as parameter which is the
         * number of impressions that can be saved in a blocking queue while waiting for
         * your ImpressionListener to log them. Of course, the larger the value of capacity,
         * the more memory can be taken up.
         * <p>
         *
         * The executor will create two threads.
         * <p>
         *
         * This is an ADVANCED function.
         *
         * @param impressionListener
         * @param queueSize maximum number of impressions that will be queued in memory. If the impressionListener is
         *                 slow, the queue will fill up and any subsequent impressions will be dropped.
         * @return this builder
         */
        @Deprecated
        public Builder impressionListener(ImpressionListener impressionListener, int queueSize) {
            if (null == _integrationsConfig) {
                _integrationsConfig = new IntegrationsConfig.Builder()
                        .impressionsListener(impressionListener, queueSize)
                        .build();
            } else {
                _integrationsConfig.addStandardImpressionListener(impressionListener, queueSize);
            }
            return this;
        }

        /**
         * The diagnostic metrics collected by the SDK are pushed back to split endpoint
         * at this period.
         * <p/>
         * This is an ADVANCED parameter
         *
         * @param seconds MUST be > 0.
         * @return this builder
         */
        public Builder metricsRefreshRate(int seconds) {
            _metricsRefreshRate = seconds;
            return this;
        }

        /**
         * Http client connection timeout. Default value is 15000ms.
         *
         * @param ms MUST be greater than 0.
         * @return this builder
         */

        public Builder connectionTimeout(int ms) {
            _connectionTimeout = ms;
            return this;
        }

        /**
         * Http client read timeout. Default value is 15000ms.
         *
         * @param ms MUST be greater than 0.
         * @return this builder
         */
        public Builder readTimeout(int ms) {
            _readTimeout = ms;
            return this;
        }

        public Builder enableDebug() {
            _debugEnabled = true;
            return this;
        }

        /**
         * Disable label capturing
         * @return this builder
         */
        public Builder disableLabels() {
            _labelsEnabled = false;
            return this;
        }

        public Builder disableIPAddress() {
            _ipAddressEnabled = false;
            return this;
        }

        /**
         * The SDK kicks off background threads to download data necessary
         * for using the SDK. You can choose to block until the SDK has
         * downloaded split definitions so that you will not get
         * the 'control' treatment.
         * <p>
         * <p>
         * If this parameter is set to a non-negative value, the SDK
         * will block for that number of milliseconds for the data to be downloaded when
         * {@link SplitClient#blockUntilReady()} or {@link SplitManager#blockUntilReady()}
         * is called
         * <p/>
         *
         * @param milliseconds MUST BE greater than or equal to 0;
         * @return this builder
         */
        public Builder setBlockUntilReadyTimeout(int milliseconds) {
            _ready = milliseconds;
            return this;
        }

        /**
         * How long to wait for impressions background thread before shutting down
         * the underlying connections.
         *
         * @param waitTime tine in milliseconds
         * @return this builder
         */
        public Builder waitBeforeShutdown(int waitTime) {
            _waitBeforeShutdown = waitTime;
            return this;
        }

        /**
         * The host location of the proxy. Default is localhost.
         *
         * @param proxyHost location of the proxy
         * @return this builder
         */
        public Builder proxyHost(String proxyHost) {
            _proxyHost = proxyHost;
            return this;
        }

        /**
         * The port of the proxy. Default is -1.
         *
         * @param proxyPort port for the proxy
         * @return this builder
         */
        public Builder proxyPort(int proxyPort) {
            _proxyPort = proxyPort;
            return this;
        }

        /**
         * Set the username for authentication against the proxy (if proxy settings are enabled). (Optional).
         *
         * @param proxyUsername
         * @return this builder
         */
        public Builder proxyUsername(String proxyUsername) {
            _proxyUsername = proxyUsername;
            return this;
        }

        /**
         * Set the password for authentication against the proxy (if proxy settings are enabled). (Optional).
         *
         * @param proxyPassword
         * @return this builder
         */
        public Builder proxyPassword(String proxyPassword) {
            _proxyPassword = proxyPassword;
            return this;
        }

        /**
         * Disables running destroy() on shutdown by default.
         *
         * @return this builder
         */
        public Builder disableDestroyOnShutDown() {
            _destroyOnShutDown = false;
            return this;
        }

        HttpHost proxy() {
            if (_proxyPort != -1) {
                return new HttpHost(_proxyHost, _proxyPort);
            }
            // Default is no proxy.
            return null;
        }

        /**
         * Set the location of the new yaml file for localhost mode defaulting to .split (legacy and deprecated format)
         * This setting is optional.
         *
         * @param splitFile location
         * @return this builder
         */
        public Builder splitFile(String splitFile) {
            _splitFile = splitFile;
            return this;
        }

        public Builder splitFile(InputStream inputStream, FileTypeEnum fileType) {
            _fileType = fileType;
            _inputStream = inputStream;
            return this;
        }

        /**
         * Set the location of the directory where are the segment json files for localhost mode.
         * This setting is optional.
         *
         * @param sementDirectory location
         * @return this builder
         */
        public Builder segmentDirectory(String sementDirectory){
            _segmentDirectory = sementDirectory;
            return this;
        }

        /**
         * Sets up integrations for the Split SDK (Currently Impressions outgoing integrations supported only).
         * @param config
         * @return
         */
        public Builder integrations(IntegrationsConfig config) {
            _integrationsConfig = config;
            return this;
        }

        /**
         * Set if streaming is enabled or not. Default is true.
         * @param streamingEnabled
         * @return
         */
        public Builder streamingEnabled(boolean streamingEnabled) {
            _streamingEnabled = streamingEnabled;
            return this;
        }

        /**
         * Set if refresh is enabled or not for localhost mode. Default is false.
         * @param localhostRefreshEnable
         * @return
         */
        public Builder localhostRefreshEnable(boolean localhostRefreshEnable) {
            _localhostRefreshEnable = localhostRefreshEnable;
            return this;
        }

        /**
         * Set how many seconds to wait before re attempting to authenticate for push notifications. Default 1 second. Minimum 1 second.
         * @param authRetryBackoffBase
         * @return
         */
        public Builder authRetryBackoffBase(int authRetryBackoffBase) {
            _authRetryBackoffBase = authRetryBackoffBase;
            return this;
        }

        /**
         * Set how many seconds to wait before re attempting to connect to streaming. Default 1 second. Minimum 1 second.
         * @param streamingReconnectBackoffBase
         * @return
         */
        public Builder streamingReconnectBackoffBase(int streamingReconnectBackoffBase) {
            _streamingReconnectBackoffBase = streamingReconnectBackoffBase;
            return this;
        }

        /**
         * Set Authentication service URL.
         * @param authServiceURL
         * @return
         */
        public Builder authServiceURL(String authServiceURL) {
            _authServiceURL = authServiceURL;
            return this;
        }

        /**
         * Set Streaming service URL.
         * @param streamingServiceURL
         * @return
         */
        public Builder streamingServiceURL(String streamingServiceURL) {
            _streamingServiceURL = streamingServiceURL;
            return this;
        }

         /** Set telemetry service URL.
         * @param telemetryURL
         * @return
         */
        public Builder telemetryURL(String telemetryURL) {
            _telemetryURl = telemetryURL;
            return this;
        }

        /**
         * How often send telemetry data
         *
         * @param telemetryRefreshRate
         * @return this builder
         */
        public Builder telemetryRefreshRate(int telemetryRefreshRate) {
            _telemetryRefreshRate = telemetryRefreshRate;
            return this;
        }

        /**
         * Type of storage
         *
         * @param mode
         * @return this builder
         */
        public Builder operationMode(OperationMode mode) {
            _operationMode = mode;
            return this;
        }

        /**
         *
         * @param storage mode
         * @return this builder
         */
        public Builder storageMode(StorageMode mode) {
            _storageMode = mode;
            return this;
        }

        /**
         * Storage wrapper
         *
         * @param customStorageWrapper
         * @return this builder
         */
        public Builder customStorageWrapper(CustomStorageWrapper customStorageWrapper) {
            _customStorageWrapper = customStorageWrapper;
            return this;
        }

        /**
         * Flag Sets Filter
         *
         * @param flagSetsFilter
         * @return this builder
         */
        public Builder flagSetsFilter(List<String> flagSetsFilter) {
            _flagSetsFilter = new LinkedHashSet<>(cleanup(flagSetsFilter));
            _invalidSetsCount = flagSetsFilter.size() - _flagSetsFilter.size();
            return this;
        }

        /**
         * User Custom Header Decorator
         *
         * @param customHeaderDecorator
         * @return this builder
         */
        public Builder customHeaderDecorator(CustomHeaderDecorator customHeaderDecorator) {
            _customHeaderDecorator = customHeaderDecorator;
            return this;
        }

        /**
         * Authentication Scheme
         *
         * @param authScheme
         * @return this builder
         */
        public Builder authScheme(HttpAuthScheme authScheme) {
            _authScheme = authScheme;
            return this;
        }

        /**
         * Kerberos Principal Account Name
         *
         * @param kerberosPrincipalName
         * @return this builder
         */
        public Builder kerberosPrincipalName(String kerberosPrincipalName) {
            _kerberosPrincipalName = kerberosPrincipalName;
            return this;
        }

        /**
         * Thread Factory
         *
         * @param threadFactory
         * @return this builder
         */
        public Builder threadFactory(ThreadFactory threadFactory) {
            _threadFactory = threadFactory;
            return this;
        }

        private void verifyRates() {
            if (_featuresRefreshRate < 5 ) {
                throw new IllegalArgumentException("featuresRefreshRate must be >= 5: " + _featuresRefreshRate);
            }

            if (_segmentsRefreshRate < 30) {
                throw new IllegalArgumentException("segmentsRefreshRate must be >= 30: " + _segmentsRefreshRate);
            }

            if (_eventSendIntervalInMillis < 1000) {
                throw new IllegalArgumentException("_eventSendIntervalInMillis must be >= 1000: " + _eventSendIntervalInMillis);
            }

            if (_metricsRefreshRate < 30) {
                throw new IllegalArgumentException("metricsRefreshRate must be >= 30: " + _metricsRefreshRate);
            }
            if(_telemetryRefreshRate < 60) {
                throw new IllegalStateException("_telemetryRefreshRate must be >= 60");
            }
        }

        private void verifyEndPoints() {
            if (_endpoint == null) {
                throw new IllegalArgumentException("endpoint must not be null");
            }

            if (_eventsEndpoint == null) {
                throw new IllegalArgumentException("events endpoint must not be null");
            }

            if (_endpointSet && !_eventsEndpointSet) {
                throw new IllegalArgumentException("If endpoint is set, you must also set the events endpoint");
            }

            if (_authServiceURL == null) {
                throw new IllegalArgumentException("authServiceURL must not be null");
            }

            if (_streamingServiceURL == null) {
                throw new IllegalArgumentException("streamingServiceURL must not be null");
            }

            if (_telemetryURl == null) {
                throw new IllegalArgumentException("telemetryURl must not be null");
            }
        }

        private void verifyAuthScheme() {
            if (_authScheme == HttpAuthScheme.KERBEROS) {
                if (proxy() == null) {
                    throw new IllegalStateException("Kerberos mode require Proxy parameters.");
                }
                if (_kerberosPrincipalName == null) {
                    throw new IllegalStateException("Kerberos mode require Kerberos Principal Name.");
                }
            }
        }

        private void verifyAllModes() {
            switch (_impressionsMode) {
                case OPTIMIZED:
                    _impressionsRefreshRate = (_impressionsRefreshRate <= 0) ? 300 : Math.max(60, _impressionsRefreshRate);
                    break;
                case DEBUG:
                    _impressionsRefreshRate = (_impressionsRefreshRate <= 0) ? 60 : _impressionsRefreshRate;
                    break;
            }

            if (_impressionsQueueSize <=0 ) {
                throw new IllegalArgumentException("impressionsQueueSize must be > 0: " + _impressionsQueueSize);
            }
            if(_storageMode == null) {
                _storageMode = StorageMode.MEMORY;
            }

            if(OperationMode.CONSUMER.equals(_operationMode)){
                if(_customStorageWrapper == null) {
                    throw new IllegalStateException("Custom Storage must not be null on Consumer mode.");
                }
                _storageMode = StorageMode.PLUGGABLE;
            }
        }

        private void verifyNetworkParams() {
            if (_connectionTimeout <= 0) {
                throw new IllegalArgumentException("connectionTimeOutInMs must be > 0: " + _connectionTimeout);
            }

            if (_readTimeout <= 0) {
                throw new IllegalArgumentException("readTimeout must be > 0: " + _readTimeout);
            }
            if (_authRetryBackoffBase <= 0) {
                throw new IllegalArgumentException("authRetryBackoffBase: must be >= 1");
            }

            if (_streamingReconnectBackoffBase <= 0) {
                throw new IllegalArgumentException("streamingReconnectBackoffBase: must be >= 1");
            }

            if (_onDemandFetchRetryDelayMs <= 0) {
                throw new IllegalStateException("streamingRetryDelay must be > 0");
            }

            if(_onDemandFetchMaxRetries <= 0) {
                throw new IllegalStateException("_onDemandFetchMaxRetries must be > 0");
            }
        }
        public SplitClientConfig build() {

            verifyRates();

            verifyAllModes();

            verifyEndPoints();

            verifyNetworkParams();

            if (_numThreadsForSegmentFetch <= 0) {
                throw new IllegalArgumentException("Number of threads for fetching segments MUST be greater than zero");
            }

            verifyAuthScheme();

                return new SplitClientConfig(
                    _endpoint,
                    _eventsEndpoint,
                    _featuresRefreshRate,
                    _segmentsRefreshRate,
                    _impressionsRefreshRate,
                    _impressionsQueueSize,
                    _impressionsMode,
                    _metricsRefreshRate,
                    _connectionTimeout,
                    _readTimeout,
                    _numThreadsForSegmentFetch,
                    _ready,
                    _debugEnabled,
                    _labelsEnabled,
                    _ipAddressEnabled,
                    _localhostRefreshEnable,
                    _waitBeforeShutdown,
                    proxy(),
                    _proxyUsername,
                    _proxyPassword,
                    _eventsQueueSize,
                    _eventSendIntervalInMillis,
                    _maxStringLength,
                    _destroyOnShutDown,
                    _splitFile,
                    _fileType,
                    _inputStream,
                    _segmentDirectory,
                    _integrationsConfig,
                    _streamingEnabled,
                    _authRetryBackoffBase,
                    _streamingReconnectBackoffBase,
                    _authServiceURL,
                    _streamingServiceURL,
                    _telemetryURl,
                    _telemetryRefreshRate,
                    _onDemandFetchRetryDelayMs,
                    _onDemandFetchMaxRetries,
                    _failedAttemptsBeforeLogging,
                    _operationMode,
                    _validateAfterInactivityInMillis,
                    STARTING_SYNC_CALL_BACKOFF_BASE_MS,
                    _customStorageWrapper,
                    _storageMode,
                    _uniqueKeysRefreshRateInMemory,
                    _uniqueKeysRefreshRateRedis,
                    _filterUniqueKeysRefreshRate,
                    _lastSeenCacheSize,
                    _threadFactory,
                    _flagSetsFilter,
                    _invalidSetsCount,
                    _customHeaderDecorator,
                    _authScheme,
                    _kerberosPrincipalName);
        }
    }
}