package io.split.telemetry.domain;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Stats {
    /* package private */ static final String FIELD_LAST_SYNCHRONIZATION = "lS";
    /* package private */ static final String FIELD_METHOD_LATENCIES = "ml";
    /* package private */ static final String FIELD_METHOD_EXCEPTIONS = "mE";
    /* package private */ static final String FIELD_HTTP_ERRORS = "hE";
    /* package private */ static final String FIELD_HTTP_LATENCIES = "hL";
    /* package private */ static final String FIELD_TOKEN_REFRESHES = "tR";
    /* package private */ static final String FIELD_AUTH_REJECTIONS = "aR";
    /* package private */ static final String FIELD_IMPRESSIONS_QUEUED = "iQ";
    /* package private */ static final String FIELD_IMPRESSIONS_DEDUPED = "iDe";
    /* package private */ static final String FIELD_IMPRESSIONS_DROPPED = "iDr";
    /* package private */ static final String FIELD_SPLITS = "spC";
    /* package private */ static final String FIELD_SEGMENTS = "seC";
    /* package private */ static final String FIELD_SEGMENTS_KEY = "skC";
    /* package private */ static final String FIELD_SESSION_LENGHT = "sL";
    /* package private */ static final String FIELD_EVENTS_QUEUED = "eQ";
    /* package private */ static final String FIELD_EVENTS_DROPPED = "eD";
    /* package private */ static final String FIELD_STREAMING_EVENT = "sE";
    /* package private */ static final String FIELD_TAGS = "t";

    @SerializedName(FIELD_LAST_SYNCHRONIZATION)
    private LastSynchronization _lastSynchronization;
    @SerializedName(FIELD_METHOD_LATENCIES)
    private MethodLatencies _methodLatencies;
    @SerializedName(FIELD_METHOD_EXCEPTIONS)
    private MethodExceptions _methodExceptions;
    @SerializedName(FIELD_HTTP_ERRORS)
    private HTTPErrors  _httpErrors;
    @SerializedName(FIELD_HTTP_LATENCIES)
    private HTTPLatencies _httpLatencies;
    @SerializedName(FIELD_TOKEN_REFRESHES)
    private long _tokenRefreshes;
    @SerializedName(FIELD_AUTH_REJECTIONS)
    private long _authRejections;
    @SerializedName(FIELD_IMPRESSIONS_QUEUED)
    private long _impressionsQueued;
    @SerializedName(FIELD_IMPRESSIONS_DEDUPED)
    private long _impressionsDeduped;
    @SerializedName(FIELD_IMPRESSIONS_DROPPED)
    private long _impressionsDropped;
    @SerializedName(FIELD_SPLITS)
    private long _splitCount;
    @SerializedName(FIELD_SEGMENTS)
    private long _segmentCount;
    @SerializedName(FIELD_SEGMENTS_KEY)
    private long _segmentKeyCount;
    @SerializedName(FIELD_SESSION_LENGHT)
    private long _sessionLengthMs;
    @SerializedName(FIELD_EVENTS_QUEUED)
    private long _eventsQueued;
    @SerializedName(FIELD_EVENTS_DROPPED)
    private long _eventsDropped;
    @SerializedName(FIELD_STREAMING_EVENT)
    private List<StreamingEvent> _streamingEvents;
    @SerializedName(FIELD_TAGS)
    private List<String> _tags;

    public LastSynchronization get_lastSynchronization() {
        return _lastSynchronization;
    }

    public void set_lastSynchronization(LastSynchronization _lastSynchronization) {
        this._lastSynchronization = _lastSynchronization;
    }

    public MethodLatencies get_methodLatencies() {
        return _methodLatencies;
    }

    public void set_methodLatencies(MethodLatencies _methodLatencies) {
        this._methodLatencies = _methodLatencies;
    }

    public MethodExceptions get_methodExceptions() {
        return _methodExceptions;
    }

    public void set_methodExceptions(MethodExceptions _methodExceptions) {
        this._methodExceptions = _methodExceptions;
    }

    public HTTPErrors get_httpErrors() {
        return _httpErrors;
    }

    public void set_httpErrors(HTTPErrors _httpErrors) {
        this._httpErrors = _httpErrors;
    }

    public HTTPLatencies get_httpLatencies() {
        return _httpLatencies;
    }

    public void set_httpLatencies(HTTPLatencies _httpLatencies) {
        this._httpLatencies = _httpLatencies;
    }

    public long get_tokenRefreshes() {
        return _tokenRefreshes;
    }

    public void set_tokenRefreshes(long _tokenRefreshes) {
        this._tokenRefreshes = _tokenRefreshes;
    }

    public long get_authRejections() {
        return _authRejections;
    }

    public void set_authRejections(long _authRejections) {
        this._authRejections = _authRejections;
    }

    public long get_impressionsQueued() {
        return _impressionsQueued;
    }

    public void set_impressionsQueued(long _impressionsQueued) {
        this._impressionsQueued = _impressionsQueued;
    }

    public long get_impressionsDeduped() {
        return _impressionsDeduped;
    }

    public void set_impressionsDeduped(long _impressionsDeduped) {
        this._impressionsDeduped = _impressionsDeduped;
    }

    public long get_impressionsDropped() {
        return _impressionsDropped;
    }

    public void set_impressionsDropped(long _impressionsDropped) {
        this._impressionsDropped = _impressionsDropped;
    }

    public long get_splitCount() {
        return _splitCount;
    }

    public void set_splitCount(long _splitCount) {
        this._splitCount = _splitCount;
    }

    public long get_segmentCount() {
        return _segmentCount;
    }

    public void set_segmentCount(long _segmentCount) {
        this._segmentCount = _segmentCount;
    }

    public long get_segmentKeyCount() {
        return _segmentKeyCount;
    }

    public void set_segmentKeyCount(long _segmentKeyCount) {
        this._segmentKeyCount = _segmentKeyCount;
    }

    public long get_sessionLengthMs() {
        return _sessionLengthMs;
    }

    public void set_sessionLengthMs(long _sessionLengthMs) {
        this._sessionLengthMs = _sessionLengthMs;
    }

    public long get_eventsQueued() {
        return _eventsQueued;
    }

    public void set_eventsQueued(long _eventsQueued) {
        this._eventsQueued = _eventsQueued;
    }

    public long get_eventsDropped() {
        return _eventsDropped;
    }

    public void set_eventsDropped(long _eventsDropped) {
        this._eventsDropped = _eventsDropped;
    }

    public List<StreamingEvent> get_streamingEvents() {
        return _streamingEvents;
    }

    public void set_streamingEvents(List<StreamingEvent> _streamingEvents) {
        this._streamingEvents = _streamingEvents;
    }

    public List<String> get_tags() {
        return _tags;
    }

    public void set_tags(List<String> _tags) {
        this._tags = _tags;
    }
}
