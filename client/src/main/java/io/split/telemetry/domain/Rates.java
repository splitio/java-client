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

    public long get_splits() {
        return _splits;
    }

    public void set_splits(long _splits) {
        this._splits = _splits;
    }

    public long get_segments() {
        return _segments;
    }

    public void set_segments(long _segments) {
        this._segments = _segments;
    }

    public long get_impressions() {
        return _impressions;
    }

    public void set_impressions(long _impressions) {
        this._impressions = _impressions;
    }

    public long get_events() {
        return _events;
    }

    public void set_events(long _events) {
        this._events = _events;
    }

    public long get_telemetry() {
        return _telemetry;
    }

    public void set_telemetry(long _telemetry) {
        this._telemetry = _telemetry;
    }
}
