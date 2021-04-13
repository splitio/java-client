package io.split.telemetry.domain;

public class AdvanceConfig {
    private int _hTTPTimeout;
    private int _segmentQueueSize;
    private int _segmentWorkers;
    private String _sdkURL;
    private String _eventsURL;
    private String _telemetryServiceURL;
    private long _eventsBulkSize;
    private int _eventsQueueSize;
    private int _impressionsQueueSize;
    private long _impressionsBulkSize;
    private boolean _streamingEnabled;
    private String _authServiceURL;
    private String _streamingServiceURL;
    private boolean _splitUpdateQueueSize;
    private long _segmentUpdateQueueSize;

    public int get_hTTPTimeout() {
        return _hTTPTimeout;
    }

    public void set_hTTPTimeout(int _hTTPTimeout) {
        this._hTTPTimeout = _hTTPTimeout;
    }

    public int get_segmentQueueSize() {
        return _segmentQueueSize;
    }

    public void set_segmentQueueSize(int _segmentQueueSize) {
        this._segmentQueueSize = _segmentQueueSize;
    }

    public int get_segmentWorkers() {
        return _segmentWorkers;
    }

    public void set_segmentWorkers(int _segmentWorkers) {
        this._segmentWorkers = _segmentWorkers;
    }

    public String get_sdkURL() {
        return _sdkURL;
    }

    public void set_sdkURL(String _sdkURL) {
        this._sdkURL = _sdkURL;
    }

    public String get_eventsURL() {
        return _eventsURL;
    }

    public void set_eventsURL(String _eventsURL) {
        this._eventsURL = _eventsURL;
    }

    public String get_telemetryServiceURL() {
        return _telemetryServiceURL;
    }

    public void set_telemetryServiceURL(String _telemetryServiceURL) {
        this._telemetryServiceURL = _telemetryServiceURL;
    }

    public long get_eventsBulkSize() {
        return _eventsBulkSize;
    }

    public void set_eventsBulkSize(long _eventsBulkSize) {
        this._eventsBulkSize = _eventsBulkSize;
    }

    public int get_eventsQueueSize() {
        return _eventsQueueSize;
    }

    public void set_eventsQueueSize(int _eventsQueueSize) {
        this._eventsQueueSize = _eventsQueueSize;
    }

    public int get_impressionsQueueSize() {
        return _impressionsQueueSize;
    }

    public void set_impressionsQueueSize(int _impressionsQueueSize) {
        this._impressionsQueueSize = _impressionsQueueSize;
    }

    public long get_impressionsBulkSize() {
        return _impressionsBulkSize;
    }

    public void set_impressionsBulkSize(long _impressionsBulkSize) {
        this._impressionsBulkSize = _impressionsBulkSize;
    }

    public boolean is_streamingEnabled() {
        return _streamingEnabled;
    }

    public void set_streamingEnabled(boolean _streamingEnabled) {
        this._streamingEnabled = _streamingEnabled;
    }

    public String get_authServiceURL() {
        return _authServiceURL;
    }

    public void set_authServiceURL(String _authServiceURL) {
        this._authServiceURL = _authServiceURL;
    }

    public String get_streamingServiceURL() {
        return _streamingServiceURL;
    }

    public void set_streamingServiceURL(String _streamingServiceURL) {
        this._streamingServiceURL = _streamingServiceURL;
    }

    public boolean is_splitUpdateQueueSize() {
        return _splitUpdateQueueSize;
    }

    public void set_splitUpdateQueueSize(boolean _splitUpdateQueueSize) {
        this._splitUpdateQueueSize = _splitUpdateQueueSize;
    }

    public long get_segmentUpdateQueueSize() {
        return _segmentUpdateQueueSize;
    }

    public void set_segmentUpdateQueueSize(long _segmentUpdateQueueSize) {
        this._segmentUpdateQueueSize = _segmentUpdateQueueSize;
    }
}
