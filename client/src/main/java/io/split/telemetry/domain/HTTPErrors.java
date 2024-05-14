package io.split.telemetry.domain;

import com.google.gson.annotations.SerializedName;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HTTPErrors {
    /* package private */ static final String FIELD_SPLIT = "sp";
    /* package private */ static final String FIELD_SEGMENTS = "se";
    /* package private */ static final String FIELD_IMPRESSIONS = "im";
    /* package private */ static final String FIELD_IMPRESSIONS_COUNT = "ic";
    /* package private */ static final String FIELD_EVENTS = "ev";
    /* package private */ static final String FIELD_TOKEN = "to";
    /* package private */ static final String FIELD_TELEMETRY = "te";

    @SerializedName(FIELD_SPLIT)
    private Map<Long, Long> _splits;
    @SerializedName(FIELD_SEGMENTS)
    private Map<Long, Long> _segments;
    @SerializedName(FIELD_IMPRESSIONS)
    private Map<Long, Long> _impressions;
    @SerializedName(FIELD_IMPRESSIONS_COUNT)
    private Map<Long, Long> _impressionsCount;
    @SerializedName(FIELD_EVENTS)
    private Map<Long, Long> _events;
    @SerializedName(FIELD_TOKEN)
    private Map<Long, Long> _token;
    @SerializedName(FIELD_TELEMETRY)
    private Map<Long, Long> _telemetry;

    public HTTPErrors() {
        _splits = new ConcurrentHashMap<>();
        _segments = new ConcurrentHashMap<>();
        _impressions = new ConcurrentHashMap<>();
        _impressionsCount = new ConcurrentHashMap<>();
        _events = new ConcurrentHashMap<>();
        _token = new ConcurrentHashMap<>();
        _telemetry = new ConcurrentHashMap<>();
    }

    public Map<Long, Long> getSplits() {
        return _splits;
    }

    public void setSplits(Map<Long, Long> _splits) {
        this._splits = _splits;
    }

    public Map<Long, Long> getSegments() {
        return _segments;
    }

    public void setSegments(Map<Long, Long> _segments) {
        this._segments = _segments;
    }

    public Map<Long, Long> getImpressions() {
        return _impressions;
    }

    public void setImpressions(Map<Long, Long> _impressions) {
        this._impressions = _impressions;
    }

    public Map<Long, Long> getEvents() {
        return _events;
    }

    public void setEvents(Map<Long, Long> _events) {
        this._events = _events;
    }

    public Map<Long, Long> getToken() {
        return _token;
    }

    public void setToken(Map<Long, Long> _token) {
        this._token = _token;
    }

    public Map<Long, Long> getTelemetry() {
        return _telemetry;
    }

    public void setTelemetry(Map<Long, Long> _telemetry) {
        this._telemetry = _telemetry;
    }

    public Map<Long, Long> getImpressionsCount() {
        return _impressionsCount;
    }

    public void setImpressionsCount(Map<Long, Long> _impressionsCount) {
        this._impressionsCount = _impressionsCount;
    }
}