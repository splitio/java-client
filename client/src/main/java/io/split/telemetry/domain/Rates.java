package io.split.telemetry.domain;

import com.google.gson.annotations.SerializedName;

public class Rates {
    /* package private */ static final String FIELD_SPLITS = "sp";
    /* package private */ static final String FIELD_SEGMENTS = "se";
    /* package private */ static final String FIELD_IMPRESSIONS = "im";
    /* package private */ static final String FIELD_EVENTS = "ev";
    /* package private */ static final String FIELD_TELEMETRY = "te";

    @SerializedName(FIELD_SPLITS)
    private long _splits;
    @SerializedName(FIELD_SEGMENTS)
    private long _segments;
    @SerializedName(FIELD_IMPRESSIONS)
    private long _impressions;
    @SerializedName(FIELD_EVENTS)
    private long _events;
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

    public long getTelemetry() {
        return _telemetry;
    }

    public void setTelemetry(long _telemetry) {
        this._telemetry = _telemetry;
    }
}