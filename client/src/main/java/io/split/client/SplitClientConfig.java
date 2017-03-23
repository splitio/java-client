package io.split.client;


import io.split.client.impressions.ImpressionListener;

import java.io.IOException;
import java.util.Properties;

/**
 * Configurations for the SplitClient.
 *
 * @author adil
 */
public class SplitClientConfig {

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
                              int impressionListenerCapacity) {
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

    public static final class Builder {

        private String _endpoint = "https://sdk.split.io";
        private boolean _endpointSet = false;
        private String _eventsEndpoint = "https://events.split.io";
        private boolean _eventsEndpointSet = false;
        private int _featuresRefreshRate = 60;
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

        public Builder() {
        }

        /**
         * The rest endpoint that sdk will hit for latest features and segments.
         *
         * @param endpoint MUST NOT be null
         * @return
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
         * @return
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
         * @return
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
         * @return
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
         * <p/>
         *
         * This is an ADVANCED parameter.
         *
         * @param impressionsQueueSize MUST be > 0. Default is 5000.
         * @return
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
         * @param capacity
         * @return
         */
        public Builder impressionListener(ImpressionListener impressionListener, int capacity) {
            _impressionListener = impressionListener;
            _impressionListenerCapacity = capacity;
            return this;
        }

        /**
         * The diagnostic metrics collected by the SDK are pushed back to split endpoint
         * at this period.
         * <p/>
         * This is an ADVANCED parameter
         *
         * @param seconds MUST be > 0.
         * @return
         */
        public Builder metricsRefreshRate(int seconds) {
            _metricsRefreshRate = seconds;
            return this;
        }

        /**
         * Http client connection timeout. Default value is 15000ms.
         *
         * @param ms MUST be greater than 0.
         * @return Builder
         */

        public Builder connectionTimeout(int ms) {
            _connectionTimeout = ms;
            return this;
        }

        /**
         * Http client read timeout. Default value is 15000ms.
         *
         * @param ms MUST be greater than 0.
         * @return this Builder instance
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
         * @return
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
         * <p/>
         * <p/>
         * If this parameter is set to a non-negative value, the SDK
         * will block for that number of milliseconds for the data to be downloaded.
         * <p/>
         * <p/>
         * If the download is not successful in this time period, a TimeOutException
         * will be thrown.
         * <p/>
         * <p/>
         * A negative value implies that the SDK building MUST NOT block. In this
         * scenario, the SDK might return the 'control' treatment until the
         * desired data has been downloaded.
         *
         * @param milliseconds MUST BE greater than or equal to 0;
         * @return
         */
        public Builder ready(int milliseconds) {
            _ready = milliseconds;
            return this;
        }


        public SplitClientConfig build() {
            if (_featuresRefreshRate < 30 ) {
                throw new IllegalArgumentException("featuresRefreshRate must be >= 30: " + _featuresRefreshRate);
            }

            if (_segmentsRefreshRate < 30) {
                throw new IllegalArgumentException("segmentsRefreshRate must be >= 30: " + _segmentsRefreshRate);
            }

            if (_impressionsRefreshRate < 30) {
                throw new IllegalArgumentException("impressionsRefreshRate must be >= 30: " + _impressionsRefreshRate);
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
                    _impressionListenerCapacity);
        }

    }


}
