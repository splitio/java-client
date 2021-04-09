package io.split.telemetry.domain;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class HTTPLatencies {
    /* package private */ static final String FIELD_SPLIT = "sp";
    /* package private */ static final String FIELD_SEGMENTS = "se";
    /* package private */ static final String FIELD_IMPRESSIONS = "im";
    /* package private */ static final String FIELD_IMPRESSIONS_COUNT = "ic";
    /* package private */ static final String FIELD_EVENTS = "ev";
    /* package private */ static final String FIELD_TOKEN = "to";
    /* package private */ static final String FIELD_TELEMETRY = "te";

    @SerializedName(FIELD_SPLIT)
    private List<Long> _splits;
    @SerializedName(FIELD_SEGMENTS)
    private List<Long>_segments;
    @SerializedName(FIELD_IMPRESSIONS)
    private List<Long> _impressions;
    @SerializedName(FIELD_IMPRESSIONS_COUNT)
    private List<Long> _impressionsCount;
    @SerializedName(FIELD_EVENTS)
    private List<Long> _events;
    @SerializedName(FIELD_TOKEN)
    private List<Long> _token;
    @SerializedName(FIELD_TELEMETRY)
    private List<Long> _telemetry;

    public HTTPLatencies() {
        _splits = new ArrayList<>();
        _segments = new ArrayList<>();
        _impressions = new ArrayList<>();
        _impressionsCount = new ArrayList<>();
        _events = new ArrayList<>();
        _token = new ArrayList<>();
        _telemetry = new ArrayList<>();
    }

    public List<Long> get_splits() {
        return _splits;
    }

    public void set_splits(List<Long> _splits) {
        this._splits = _splits;
    }

    public List<Long> get_segments() {
        return _segments;
    }

    public void set_segments(List<Long> _segments) {
        this._segments = _segments;
    }

    public List<Long> get_impressions() {
        return _impressions;
    }

    public void set_impressions(List<Long> _impressions) {
        this._impressions = _impressions;
    }

    public List<Long> get_events() {
        return _events;
    }

    public void set_events(List<Long> _events) {
        this._events = _events;
    }

    public List<Long> get_token() {
        return _token;
    }

    public void set_token(List<Long> _token) {
        this._token = _token;
    }

    public List<Long> get_telemetry() {
        return _telemetry;
    }

    public void set_telemetry(List<Long> _telemetry) {
        this._telemetry = _telemetry;
    }

    public List<Long> get_impressionsCount() {
        return _impressionsCount;
    }

    public void set_impressionsCount(List<Long> _impressionsCount) {
        this._impressionsCount = _impressionsCount;
    }
}
