package io.split.telemetry.domain;

import com.google.gson.annotations.SerializedName;

public class URLOverrides {
    /* package private */ static final String FIELD_SDK = "s";
    /* package private */ static final String FIELD_EVENTS = "e";
    /* package private */ static final String FIELD_AUTH = "a";
    /* package private */ static final String FIELD_STREAM = "st";
    /* package private */ static final String FIELD_TELEMETRY = "t";

    @SerializedName(FIELD_SDK)
    private boolean _sdk;
    @SerializedName(FIELD_EVENTS)
    private boolean _events;
    @SerializedName(FIELD_AUTH)
    private boolean _auth;
    @SerializedName(FIELD_STREAM)
    private boolean _stream;
    @SerializedName(FIELD_TELEMETRY)
    private boolean _telemetry;

    public boolean is_sdk() {
        return _sdk;
    }

    public void set_sdk(boolean _sdk) {
        this._sdk = _sdk;
    }

    public boolean is_events() {
        return _events;
    }

    public void set_events(boolean _events) {
        this._events = _events;
    }

    public boolean is_auth() {
        return _auth;
    }

    public void set_auth(boolean _auth) {
        this._auth = _auth;
    }

    public boolean is_stream() {
        return _stream;
    }

    public void set_stream(boolean _stream) {
        this._stream = _stream;
    }

    public boolean is_telemetry() {
        return _telemetry;
    }

    public void set_telemetry(boolean _telemetry) {
        this._telemetry = _telemetry;
    }
}
