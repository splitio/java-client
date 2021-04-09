package io.split.telemetry.domain;

import com.google.gson.annotations.SerializedName;

public class StreamingEvent {
    /* package private */ static final String FIELD_TYPE = "sp";
    /* package private */ static final String FIELD_DATA = "se";
    /* package private */ static final String FIELD_TIMESTAMP = "im";

    @SerializedName(FIELD_TYPE)
    private int _type;
    @SerializedName(FIELD_DATA)
    private long _data;
    @SerializedName(FIELD_TIMESTAMP)
    private long timestamp;

    public int get_type() {
        return _type;
    }

    public void set_type(int _type) {
        this._type = _type;
    }

    public long get_data() {
        return _data;
    }

    public void set_data(long _data) {
        this._data = _data;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
