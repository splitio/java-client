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

    public List<Long> getSplits() {
        return _splits;
    }

    public void setSplits(List<Long> _splits) {
        this._splits = _splits;
    }

    public List<Long> getSegments() {
        return _segments;
    }

    public void setSegments(List<Long> _segments) {
        this._segments = _segments;
    }

    public List<Long> getImpressions() {
        return _impressions;
    }

    public void setImpressions(List<Long> _impressions) {
        this._impressions = _impressions;
    }

    public List<Long> getEvents() {
        return _events;
    }

    public void setEvents(List<Long> _events) {
        this._events = _events;
    }

    public List<Long> getToken() {
        return _token;
    }

    public void setToken(List<Long> _token) {
        this._token = _token;
    }

    public List<Long> getTelemetry() {
        return _telemetry;
    }

    public void setTelemetry(List<Long> _telemetry) {
        this._telemetry = _telemetry;
    }

    public List<Long> getImpressionsCount() {
        return _impressionsCount;
    }

    public void setImpressionsCount(List<Long> _impressionsCount) {
        this._impressionsCount = _impressionsCount;
    }
}