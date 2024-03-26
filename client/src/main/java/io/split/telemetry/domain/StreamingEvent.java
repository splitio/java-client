package io.split.telemetry.domain;

import com.google.gson.annotations.SerializedName;

public class StreamingEvent {
    /* package private */ static final String FIELD_TYPE = "e";
    /* package private */ static final String FIELD_DATA = "d";
    /* package private */ static final String FIELD_TIMESTAMP = "t";

    @SerializedName(FIELD_TYPE)
    private int _type;
    @SerializedName(FIELD_DATA)
    private long _data;
    @SerializedName(FIELD_TIMESTAMP)
    private long _timestamp;

    public StreamingEvent(int _type, long _data, long _timestamp) {
        this._type = _type;
        this._data = _data;
        this._timestamp = _timestamp;
    }

    public int getType() {
        return _type;
    }

    public void setType(int _type) {
        this._type = _type;
    }

    public long getData() {
        return _data;
    }

    public void setData(long _data) {
        this._data = _data;
    }

    public long getTimestamp() {
        return _timestamp;
    }

    public void setTimestamp(long timestamp) {
        this._timestamp = timestamp;
    }
}