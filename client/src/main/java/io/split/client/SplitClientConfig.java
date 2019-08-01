package io.split.client;


import io.split.client.impressions.ImpressionListener;
import org.apache.http.HttpHost;

import java.io.IOException;
import java.util.Properties;

/**
 * Configurations for the SplitClient.
 *
 * @author adil
 */
public class SplitClientConfig {

    public static final String LOCALHOST_DEFAULT_FILE = "split.yaml";

    private final String _endpoint;
    private final String _eventsEndpoint;

    private final int _featuresRefreshRate;
    private final int _segmentsRefreshRate;
    private final int _impressionsRefreshRate;
    private final int _impressionsQueueSize;
    private final int _metricsRefreshRate;
    private final int _connectionTimeout;
    private final int _readTimeout;
    private final int _numThreadsForSegmentFetch;
    private final boolean _debugEnabled;
    private final boolean _labelsEnabled;
    private final int _ready;
    private final ImpressionListener _impressionListener;
    private final int _impressionListenerCapacity;
    private final int _waitBeforeShutdown;
    private final int _eventsQueueSize;
    private final long _eventFlushIntervalInMillis;
    private final int _maxStringLength;
    private final boolean _destroyOnShutDown;
    private final String _splitFile;

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
                              int metricsRefreshRate,
                              int connectionTimeout,
                              int readTimeout,
                              int numThreadsForSegmentFetch,
                              int ready,
                              boolean debugEnabled,
                              boolean labelsEnabled,
                              ImpressionListener impressionListener,
                              int impressionListenerCapacity,
                              int waitBeforeShutdown,
                              HttpHost proxy,
                              String proxyUsername,
                              String proxyPassword,
                              int eventsQueueSize,
                              long eventFlushIntervalInMillis,
                              int maxStringLength,
                              boolean destroyOnShutDown,
                              String splitFile) {
        _endpoint = endpoint;
        _eventsEndpoint = eventsEndpoint;
        _featuresRefreshRate = pollForFeatureChangesEveryNSeconds;
        _segmentsRefreshRate = segmentsRefreshRate;
        _impressionsRefreshRate = impressionsRefreshRate;
        _impressionsQueueSize = impressionsQueueSize;
        _metricsRefreshRate = metricsRefreshRate;
        _connectionTimeout = connectionTimeout;
        _readTimeout = readTimeout;
        _numThreadsForSegmentFetch = numThreadsForSegmentFetch;
        _ready = ready;
        _debugEnabled = debugEnabled;
        _labelsEnabled = labelsEnabled;
        _impressionListener = impressionListener;
        _impressionListenerCapacity = impressionListenerCapacity;
        _waitBeforeShutdown = waitBeforeShutdown;
        _proxy = proxy;
        _proxyUsername = proxyUsername;
        _proxyPassword = proxyPassword;
        _eventsQueueSize = eventsQueueSize;
        _eventFlushIntervalInMillis = eventFlushIntervalInMillis;
        _maxStringLength = maxStringLength;
        _destroyOnShutDown = destroyOnShutDown;
        _splitFile = splitFile;

        Properties props = new Properties();
        try {
            props.load(this.getClass().getClassLoader().getResourceAsStream("version.properties"));
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

    public int blockUntilReady() {
        return _ready;
    }

    public ImpressionListener impressionListener() {
        return _impressionListener;
    }

    public int impressionListenerCapactity() {
        return _impressionListenerCapacity;
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

    public static final class Builder {

        private String _endpoint = "https://sdk.split.io";
        private boolean _endpointSet = false;
        private String _eventsEndpoint = "https://events.split.io";
        private boolean _eventsEndpointSet = false;
        private int _featuresRefreshRate = 5;
        private int _segmentsRefreshRate = 60;
        private int _impressionsRefreshRate = 30;
        private int _impressionsQueueSize = 30000;
        private int _connectionTimeout = 15000;
        private int _readTimeout = 15000;
        private int _numThreadsForSegmentFetch = 2;
        private boolean _debugEnabled = false;
        private int _ready = -1; // -1 means no blocking
        private int _metricsRefreshRate = 60;
        private boolean _labelsEnabled = true;
        private ImpressionListener _impressionListener;
        private int _impressionListenerCapacity;
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
        public Builder impressionListener(ImpressionListener impressionListener, int queueSize) {
            _impressionListener = impressionListener;
            _impressionListenerCapacity = queueSize;
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


        public SplitClientConfig build() {
            if (_featuresRefreshRate < 5 ) {
                throw new IllegalArgumentException("featuresRefreshRate must be >= 5: " + _featuresRefreshRate);
            }

            if (_segmentsRefreshRate < 30) {
                throw new IllegalArgumentException("segmentsRefreshRate must be >= 30: " + _segmentsRefreshRate);
            }

            if (_impressionsRefreshRate <= 0) {
                throw new IllegalArgumentException("impressionsRefreshRate must be > 0: " + _impressionsRefreshRate);
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


            if (_impressionListener != null) {
                if (_impressionListenerCapacity <= 0) {
                    throw new IllegalArgumentException("An ImpressionListener was provided, but its capacity was non-positive: " + _impressionListenerCapacity);
                }
            }

            return new SplitClientConfig(
                    _endpoint,
                    _eventsEndpoint,
                    _featuresRefreshRate,
                    _segmentsRefreshRate,
                    _impressionsRefreshRate,
                    _impressionsQueueSize,
                    _metricsRefreshRate,
                    _connectionTimeout,
                    _readTimeout,
                    _numThreadsForSegmentFetch,
                    _ready,
                    _debugEnabled,
                    _labelsEnabled,
                    _impressionListener,
                    _impressionListenerCapacity,
                    _waitBeforeShutdown,
                    proxy(),
                    _proxyUsername,
                    _proxyPassword,
                    _eventsQueueSize,
                    _eventFlushIntervalInMillis,
                    _maxStringLength,
                    _destroyOnShutDown,
                    _splitFile);
        }

    }


}
