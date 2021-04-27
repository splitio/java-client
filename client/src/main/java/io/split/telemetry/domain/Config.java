package io.split.telemetry.domain;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Config {
    /* package private */ static final String FIELD_OPERATION_MODE = "oM";
    /* package private */ static final String FIELD_STREAMING_ENABLED = "sE";
    /* package private */ static final String FIELD_STORAGE = "st";
    /* package private */ static final String FIELD_RATES = "rR";
    /* package private */ static final String FIELD_URL_OVERRIDES = "uO";
    /* package private */ static final String FIELD_IMPRESSIONS_QUEUE = "iQ";
    /* package private */ static final String FIELD_EVENT_QUEUE = "eQ";
    /* package private */ static final String FIELD_IMPRESSIONS_MODE = "iM";
    /* package private */ static final String FIELD_IMPRESSIONS_LISTENER = "iL";
    /* package private */ static final String FIELD_HTTP_PROXY_DETECTED = "hP";
    /* package private */ static final String FIELD_ACTIVE_FACTORIES = "aF";
    /* package private */ static final String FIELD_REDUNDANT_FACTORIES = "rF";
    /* package private */ static final String FIELD_TIME_UNTIL_READY = "tR";
    /* package private */ static final String FIELD_BUR_TIMEOUTS = "bT";
    /* package private */ static final String FIELD_NON_READY_USAGES = "nR";
    /* package private */ static final String FIELD_INTEGRATIONS = "i";
    /* package private */ static final String FIELD__TAGS = "t";

    @SerializedName(FIELD_OPERATION_MODE)
    private int _operationMode;
    @SerializedName(FIELD_STREAMING_ENABLED)
    private boolean _streamingEnabled;
    @SerializedName(FIELD_STORAGE)
    private String _storage;
    @SerializedName(FIELD_RATES)
    private Rates _rates;
    @SerializedName(FIELD_URL_OVERRIDES)
    private URLOverrides _urlOverrides;
    @SerializedName(FIELD_IMPRESSIONS_QUEUE)
    private long _impressionsQueueSize;
    @SerializedName(FIELD_EVENT_QUEUE)
    private long _eventsQueueSize;
    @SerializedName(FIELD_IMPRESSIONS_MODE)
    private int _impressionsMode;
    @SerializedName(FIELD_IMPRESSIONS_LISTENER)
    private boolean _impressionsListenerEnabled;
    @SerializedName(FIELD_HTTP_PROXY_DETECTED)
    private boolean _httpProxyDetected;
    @SerializedName(FIELD_ACTIVE_FACTORIES)
    private long _activeFactories;
    @SerializedName(FIELD_REDUNDANT_FACTORIES)
    private long _redundantFactories;
    @SerializedName(FIELD_TIME_UNTIL_READY)
    private long _timeUntilReady;
    @SerializedName(FIELD_BUR_TIMEOUTS)
    private long _burTimeouts;
    @SerializedName(FIELD_NON_READY_USAGES)
    private long _nonReadyUsages;
    @SerializedName(FIELD_INTEGRATIONS)
    private List<String> _integrations;
    @SerializedName(FIELD__TAGS)
    private List<String> _tags;

    public int get_operationMode() {
        return _operationMode;
    }

    public void set_operationMode(int _operationMode) {
        this._operationMode = _operationMode;
    }

    public boolean is_streamingEnabled() {
        return _streamingEnabled;
    }

    public void set_streamingEnabled(boolean _streamingEnabled) {
        this._streamingEnabled = _streamingEnabled;
    }

    public String get_storage() {
        return _storage;
    }

    public void set_storage(String _storage) {
        this._storage = _storage;
    }

    public Rates get_rates() {
        return _rates;
    }

    public void set_rates(Rates _rates) {
        this._rates = _rates;
    }

    public URLOverrides get_urlOverrides() {
        return _urlOverrides;
    }

    public void set_urlOverrides(URLOverrides _urlOverrides) {
        this._urlOverrides = _urlOverrides;
    }

    public long get_impressionsQueueSize() {
        return _impressionsQueueSize;
    }

    public void set_impressionsQueueSize(long _impressionsQueueSize) {
        this._impressionsQueueSize = _impressionsQueueSize;
    }

    public long get_eventsQueueSize() {
        return _eventsQueueSize;
    }

    public void set_eventsQueueSize(long _eventsQueueSize) {
        this._eventsQueueSize = _eventsQueueSize;
    }

    public int get_impressionsMode() {
        return _impressionsMode;
    }

    public void set_impressionsMode(int _impressionsMode) {
        this._impressionsMode = _impressionsMode;
    }

    public boolean is_impressionsListenerEnabled() {
        return _impressionsListenerEnabled;
    }

    public void set_impressionsListenerEnabled(boolean _impressionsListenerEnabled) {
        this._impressionsListenerEnabled = _impressionsListenerEnabled;
    }

    public boolean is_httpProxyDetected() {
        return _httpProxyDetected;
    }

    public void set_httpProxyDetected(boolean _httpProxyDetected) {
        this._httpProxyDetected = _httpProxyDetected;
    }

    public long get_activeFactories() {
        return _activeFactories;
    }

    public void set_activeFactories(long _activeFactories) {
        this._activeFactories = _activeFactories;
    }

    public long get_redundantFactories() {
        return _redundantFactories;
    }

    public void set_redundantFactories(long _redundantFactories) {
        this._redundantFactories = _redundantFactories;
    }

    public long get_timeUntilReady() {
        return _timeUntilReady;
    }

    public void set_timeUntilReady(long _timeUntilReady) {
        this._timeUntilReady = _timeUntilReady;
    }

    public long get_burTimeouts() {
        return _burTimeouts;
    }

    public void set_burTimeouts(long _burTimeouts) {
        this._burTimeouts = _burTimeouts;
    }

    public long get_nonReadyUsages() {
        return _nonReadyUsages;
    }

    public void set_nonReadyUsages(long _nonReadyUsages) {
        this._nonReadyUsages = _nonReadyUsages;
    }

    public List<String> get_integrations() {
        return _integrations;
    }

    public void set_integrations(List<String> _integrations) {
        this._integrations = _integrations;
    }

    public List<String> get_tags() {
        return _tags;
    }

    public void set_tags(List<String> _tags) {
        this._tags = _tags;
    }
}
