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
    /* package private */ static final String FIELD_TAGS = "t";
    /* package private */ static final String FIELD_FLAG_SETS_TOTAL = "fsT";
    /* package private */ static final String FIELD_FLAG_SETS_INVALID = "fsI";

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
    @SerializedName(FIELD_TAGS)
    private List<String> _tags;
    @SerializedName(FIELD_FLAG_SETS_TOTAL)
    private int _flagSetsTotal;
    @SerializedName(FIELD_FLAG_SETS_INVALID)
    private int _flagSetsInvalid;

    public int getOperationMode() {
        return _operationMode;
    }

    public void setOperationMode(int operationMode) {
        this._operationMode = operationMode;
    }

    public boolean isStreamingEnabled() {
        return _streamingEnabled;
    }

    public void setStreamingEnabled(boolean streamingEnabled) {
        this._streamingEnabled = streamingEnabled;
    }

    public String getStorage() {
        return _storage;
    }

    public void setStorage(String storage) {
        this._storage = storage;
    }

    public Rates getRates() {
        return _rates;
    }

    public void setRates(Rates rates) {
        this._rates = rates;
    }

    public URLOverrides getUrlOverrides() {
        return _urlOverrides;
    }

    public void setUrlOverrides(URLOverrides urlOverrides) {
        this._urlOverrides = urlOverrides;
    }

    public long getImpressionsQueueSize() {
        return _impressionsQueueSize;
    }

    public void setImpressionsQueueSize(long impressionsQueueSize) {
        this._impressionsQueueSize = impressionsQueueSize;
    }

    public long getEventsQueueSize() {
        return _eventsQueueSize;
    }

    public void setEventsQueueSize(long eventsQueueSize) {
        this._eventsQueueSize = eventsQueueSize;
    }

    public int getImpressionsMode() {
        return _impressionsMode;
    }

    public void setImpressionsMode(int impressionsMode) {
        this._impressionsMode = impressionsMode;
    }

    public boolean isImpressionsListenerEnabled() {
        return _impressionsListenerEnabled;
    }

    public void setImpressionsListenerEnabled(boolean impressionsListenerEnabled) {
        this._impressionsListenerEnabled = impressionsListenerEnabled;
    }

    public boolean isHttpProxyDetected() {
        return _httpProxyDetected;
    }

    public void setHttpProxyDetected(boolean httpProxyDetected) {
        this._httpProxyDetected = httpProxyDetected;
    }

    public long getActiveFactories() {
        return _activeFactories;
    }

    public void setActiveFactories(long activeFactories) {
        this._activeFactories = activeFactories;
    }

    public long getRedundantFactories() {
        return _redundantFactories;
    }

    public void setRedundantFactories(long redundantFactories) {
        this._redundantFactories = redundantFactories;
    }

    public long getTimeUntilReady() {
        return _timeUntilReady;
    }

    public void setTimeUntilReady(long timeUntilReady) {
        this._timeUntilReady = timeUntilReady;
    }

    public long getBurTimeouts() {
        return _burTimeouts;
    }

    public void setBurTimeouts(long burTimeouts) {
        this._burTimeouts = burTimeouts;
    }

    public long getNonReadyUsages() {
        return _nonReadyUsages;
    }

    public void setNonReadyUsages(long nonReadyUsages) {
        this._nonReadyUsages = nonReadyUsages;
    }

    public List<String> getIntegrations() {
        return _integrations;
    }

    public void setIntegrations(List<String> integrations) {
        this._integrations = integrations;
    }

    public List<String> getTags() {
        return _tags;
    }

    public void setTags(List<String> tags) {
        this._tags = tags;
    }

    public int getFlagSetsTotal() {
        return _flagSetsTotal;
    }

    public int getFlagSetsInvalid() {
        return _flagSetsInvalid;
    }

    public void setFlagSetsTotal(int flagSetsTotal) {
        this._flagSetsTotal = flagSetsTotal;
    }

    public void setFlagSetsInvalid(int flagSetsInvalid) {
        this._flagSetsInvalid = flagSetsInvalid;
    }
}
