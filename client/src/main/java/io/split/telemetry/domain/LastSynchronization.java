package io.split.telemetry.domain;

import com.google.gson.annotations.SerializedName;

public class LastSynchronization {
    /* package private */ static final String FIELD_SPLIT = "sp";
    /* package private */ static final String FIELD_SEGMENTS = "se";
    /* package private */ static final String FIELD_IMPRESSIONS = "im";
    /* package private */ static final String FIELD_IMPRESSIONS_COUNT = "ic";
    /* package private */ static final String FIELD_EVENTS = "ev";
    /* package private */ static final String FIELD_TOKEN = "to";
    /* package private */ static final String FIELD_TELEMETRY = "te";

    @SerializedName(FIELD_SPLIT)
    private long _splits;
    @SerializedName(FIELD_SEGMENTS)
    private long _segments;
    @SerializedName(FIELD_IMPRESSIONS)
    private long _impressions;
    @SerializedName(FIELD_IMPRESSIONS_COUNT)
    private long _impressionsCount;
    @SerializedName(FIELD_EVENTS)
    private long _events;
    @SerializedName(FIELD_TOKEN)
    private long _token;
    @SerializedName(FIELD_TELEMETRY)
    private long _telemetry;

    public long getSplits() {
        return _splits;
    }

    public void setSplits(long _splits) {
        this._splits = _splits;
    }

    public long getSegments() {
        return _segments;
    }

    public void setSegments(long _segments) {
        this._segments = _segments;
    }

    public long getImpressions() {
        return _impressions;
    }

    public void setImpressions(long _impressions) {
        this._impressions = _impressions;
    }

    public long getEvents() {
        return _events;
    }

    public void setEvents(long _events) {
        this._events = _events;
    }

    public long getToken() {
        return _token;
    }

    public void setToken(long _token) {
        this._token = _token;
    }

    public long getTelemetry() {
        return _telemetry;
    }

    public void setTelemetry(long _telemetry) {
        this._telemetry = _telemetry;
    }

    public long getImpressionsCount() {
        return _impressionsCount;
    }

    public void setImpressionsCount(long _impressionsCount) {
        this._impressionsCount = _impressionsCount;
    }
}