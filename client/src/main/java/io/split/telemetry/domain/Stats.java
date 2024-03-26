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
    /* package private */ static final String FIELD_UPDATES_FROM_SSE = "ufs";

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
    @SerializedName(FIELD_UPDATES_FROM_SSE)
    private UpdatesFromSSE _updatesFromSSE;

    public LastSynchronization getLastSynchronization() {
        return _lastSynchronization;
    }

    public void setLastSynchronization(LastSynchronization lastSynchronization) {
        this._lastSynchronization = lastSynchronization;
    }

    public MethodLatencies getMethodLatencies() {
        return _methodLatencies;
    }

    public void setMethodLatencies(MethodLatencies methodLatencies) {
        this._methodLatencies = methodLatencies;
    }

    public MethodExceptions getMethodExceptions() {
        return _methodExceptions;
    }

    public void setMethodExceptions(MethodExceptions methodExceptions) {
        this._methodExceptions = methodExceptions;
    }

    public HTTPErrors getHttpErrors() {
        return _httpErrors;
    }

    public void setHttpErrors(HTTPErrors httpErrors) {
        this._httpErrors = httpErrors;
    }

    public HTTPLatencies getHttpLatencies() {
        return _httpLatencies;
    }

    public void setHttpLatencies(HTTPLatencies httpLatencies) {
        this._httpLatencies = httpLatencies;
    }

    public long getTokenRefreshes() {
        return _tokenRefreshes;
    }

    public void setTokenRefreshes(long tokenRefreshes) {
        this._tokenRefreshes = tokenRefreshes;
    }

    public long getAuthRejections() {
        return _authRejections;
    }

    public void setAuthRejections(long authRejections) {
        this._authRejections = authRejections;
    }

    public long getImpressionsQueued() {
        return _impressionsQueued;
    }

    public void setImpressionsQueued(long impressionsQueued) {
        this._impressionsQueued = impressionsQueued;
    }

    public long getImpressionsDeduped() {
        return _impressionsDeduped;
    }

    public void setImpressionsDeduped(long impressionsDeduped) {
        this._impressionsDeduped = impressionsDeduped;
    }

    public long getImpressionsDropped() {
        return _impressionsDropped;
    }

    public void setImpressionsDropped(long impressionsDropped) {
        this._impressionsDropped = impressionsDropped;
    }

    public long getSplitCount() {
        return _splitCount;
    }

    public void setSplitCount(long splitCount) {
        this._splitCount = splitCount;
    }

    public long getSegmentCount() {
        return _segmentCount;
    }

    public void setSegmentCount(long segmentCount) {
        this._segmentCount = segmentCount;
    }

    public long getSegmentKeyCount() {
        return _segmentKeyCount;
    }

    public void setSegmentKeyCount(long segmentKeyCount) {
        this._segmentKeyCount = segmentKeyCount;
    }

    public long getSessionLengthMs() {
        return _sessionLengthMs;
    }

    public void setSessionLengthMs(long sessionLengthMs) {
        this._sessionLengthMs = sessionLengthMs;
    }

    public long getEventsQueued() {
        return _eventsQueued;
    }

    public void setEventsQueued(long eventsQueued) {
        this._eventsQueued = eventsQueued;
    }

    public long getEventsDropped() {
        return _eventsDropped;
    }

    public void setEventsDropped(long eventsDropped) {
        this._eventsDropped = eventsDropped;
    }

    public List<StreamingEvent> getStreamingEvents() {
        return _streamingEvents;
    }

    public void setStreamingEvents(List<StreamingEvent> streamingEvents) {
        this._streamingEvents = streamingEvents;
    }

    public List<String> getTags() {
        return _tags;
    }

    public void setTags(List<String> tags) {
        this._tags = tags;
    }

    public UpdatesFromSSE getUpdatesFromSSE() {
        return _updatesFromSSE;
    }

    public void setUpdatesFromSSE(UpdatesFromSSE updatesFromSSE) {
        this._updatesFromSSE = updatesFromSSE;
    }
}