package io.split.client;


import io.split.client.impressions.ImpressionListener;
import io.split.client.impressions.ImpressionsManager;
import io.split.integrations.IntegrationsConfig;
import org.apache.hc.core5.http.HttpHost;

import java.io.IOException;
import java.util.Properties;

/**
 * Configurations for the SplitClient.
 *
 * @author adil
 */
public class SplitClientConfig {

    public static final String LOCALHOST_DEFAULT_FILE = "split.yaml";
    public static final String SDK_ENDPOINT = "https://sdk.split.io";
    public static final String EVENTS_ENDPOINT = "https://events.split.io";
    public static final String AUTH_ENDPOINT = "https://auth.split.io/api/auth";
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
    private final int _ready;
    private final int _waitBeforeShutdown;
    private final int _eventsQueueSize;
    private final long _eventFlushIntervalInMillis;
    private final int _maxStringLength;
    private final boolean _destroyOnShutDown;
    private final String _splitFile;
    private final IntegrationsConfig _integrationsConfig;
    private final boolean _streamingEnabled;
    private final int _authRetryBackoffBase;
    private final int _streamingReconnectBackoffBase;
    private final String _authServiceURL;
    private final String _streamingServiceURL;
    private final String _telemetryURL;
    private final int _telemetryRefreshRate;
    private final int _onDemandFetchRetryDelayMs;

    // Proxy configs
    private final HttpHost _proxy;
    private final String _proxyUsername;
    private final String _proxyPassword;

    // To be set during startup
    public static String splitSdkVersion;


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
                              int waitBeforeShutdown,
                              HttpHost proxy,
                              String proxyUsername,
                              String proxyPassword,
                              int eventsQueueSize,
                              long eventFlushIntervalInMillis,
                              int maxStringLength,
                              boolean destroyOnShutDown,
                              String splitFile,
                              IntegrationsConfig integrationsConfig,
                              boolean streamingEnabled,
                              int authRetryBackoffBase,
                              int streamingReconnectBackoffBase,
                              String authServiceURL,
                              String streamingServiceURL,
                              String telemetryURL,
                              int telemetryRefreshRate,
                              int onDemandFetchRetryDelayMs) {
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
        _waitBeforeShutdown = waitBeforeShutdown;
        _proxy = proxy;
        _proxyUsername = proxyUsername;
        _proxyPassword = proxyPassword;
        _eventsQueueSize = eventsQueueSize;
        _eventFlushIntervalInMillis = eventFlushIntervalInMillis;
        _maxStringLength = maxStringLength;
        _destroyOnShutDown = destroyOnShutDown;
        _splitFile = splitFile;
        _integrationsConfig = integrationsConfig;
        _streamingEnabled = streamingEnabled;
        _authRetryBackoffBase = authRetryBackoffBase;
        _streamingReconnectBackoffBase = streamingReconnectBackoffBase;
        _authServiceURL = authServiceURL;
        _streamingServiceURL = streamingServiceURL;
        _telemetryURL = telemetryURL;
        _telemetryRefreshRate = telemetryRefreshRate;
        _onDemandFetchRetryDelayMs = onDemandFetchRetryDelayMs;

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

    public long eventFlushIntervalInMillis() {
        return _eventFlushIntervalInMillis;
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

    public String get_telemetryURL() {
        return _telemetryURL;
    }

    public int get_telemetryRefreshRate() {
        return _telemetryRefreshRate;
    }
    public int streamingRetryDelay() {return _onDemandFetchRetryDelayMs;}

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
        private int _numThreadsForSegmentFetch = 2;
        private boolean _debugEnabled = false;
        private int _ready = -1; // -1 means no blocking
        private int _metricsRefreshRate = 60;
        private boolean _labelsEnabled = true;
        private  boolean _ipAddressEnabled = true;
        private int _waitBeforeShutdown = 5000;
        private String _proxyHost = "localhost";
        private int _proxyPort = -1;
        private String _proxyUsername;
        private String _proxyPassword;
        private int _eventsQueueSize = 500;
        private long _eventFlushIntervalInMillis = 30 * 1000;
        private int _maxStringLength = 250;
        private boolean _destroyOnShutDown = true;
        private String _splitFile = null;
        private IntegrationsConfig _integrationsConfig = null;
        private boolean _streamingEnabled = true;
        private int _authRetryBackoffBase = 1;
        private int _streamingReconnectBackoffBase = 1;
        private String _authServiceURL = AUTH_ENDPOINT;
        private String _streamingServiceURL = STREAMING_ENDPOINT;
        private String _telemetryURl = TELEMETRY_ENDPOINT;
        private int _telemetryRefreshRate = 60;
        private int _onDemandFetchRetryDelayMs = 50;

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
            _eventFlushIntervalInMillis = eventFlushIntervalInMillis;
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

        /**
         * Set telemetry service URL.
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

        public SplitClientConfig build() {
            if (_featuresRefreshRate < 5 ) {
                throw new IllegalArgumentException("featuresRefreshRate must be >= 5: " + _featuresRefreshRate);
            }

            if (_segmentsRefreshRate < 30) {
                throw new IllegalArgumentException("segmentsRefreshRate must be >= 30: " + _segmentsRefreshRate);
            }

            switch (_impressionsMode) {
                case OPTIMIZED:
                    _impressionsRefreshRate = (_impressionsRefreshRate <= 0) ? 300 : Math.max(60, _impressionsRefreshRate);
                    break;
                case DEBUG:
                    _impressionsRefreshRate = (_impressionsRefreshRate <= 0) ? 60 : _impressionsRefreshRate;
                    break;
            }

            if (_eventFlushIntervalInMillis < 1000) {
                throw new IllegalArgumentException("_eventFlushIntervalInMillis must be >= 1000: " + _eventFlushIntervalInMillis);
            }

            if (_metricsRefreshRate < 30) {
                throw new IllegalArgumentException("metricsRefreshRate must be >= 30: " + _metricsRefreshRate);
            }

            if (_impressionsQueueSize <=0 ) {
                throw new IllegalArgumentException("impressionsQueueSize must be > 0: " + _impressionsQueueSize);
            }

            if (_connectionTimeout <= 0) {
                throw new IllegalArgumentException("connectionTimeOutInMs must be > 0: " + _connectionTimeout);
            }

            if (_readTimeout <= 0) {
                throw new IllegalArgumentException("readTimeout must be > 0: " + _readTimeout);
            }

            if (_endpoint == null) {
                throw new IllegalArgumentException("endpoint must not be null");
            }

            if (_eventsEndpoint == null) {
                throw new IllegalArgumentException("events endpoint must not be null");
            }

            if (_endpointSet && !_eventsEndpointSet) {
                throw new IllegalArgumentException("If endpoint is set, you must also set the events endpoint");
            }

            if (_numThreadsForSegmentFetch <= 0) {
                throw new IllegalArgumentException("Number of threads for fetching segments MUST be greater than zero");
            }

            if (_authRetryBackoffBase <= 0) {
                throw new IllegalArgumentException("authRetryBackoffBase: must be >= 1");
            }

            if (_streamingReconnectBackoffBase <= 0) {
                throw new IllegalArgumentException("streamingReconnectBackoffBase: must be >= 1");
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

            if (_onDemandFetchRetryDelayMs <= 0) {
                throw new IllegalStateException("streamingRetryDelay must be > 0");
            }

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
                        _waitBeforeShutdown,
                        proxy(),
                        _proxyUsername,
                        _proxyPassword,
                        _eventsQueueSize,
                        _eventFlushIntervalInMillis,
                        _maxStringLength,
                        _destroyOnShutDown,
                        _splitFile,
                        _integrationsConfig,
                        _streamingEnabled,
                        _authRetryBackoffBase,
                        _streamingReconnectBackoffBase,
                        _authServiceURL,
                        _streamingServiceURL,
                        _telemetryURl,
                        _telemetryRefreshRate,
                        _onDemandFetchRetryDelayMs);
            }
    }
}
