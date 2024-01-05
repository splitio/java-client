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

    public boolean isSdk() {
        return _sdk;
    }

    public void setSdk(boolean _sdk) {
        this._sdk = _sdk;
    }

    public boolean isEvents() {
        return _events;
    }

    public void setEvents(boolean _events) {
        this._events = _events;
    }

    public boolean isAuth() {
        return _auth;
    }

    public void setAuth(boolean _auth) {
        this._auth = _auth;
    }

    public boolean isStream() {
        return _stream;
    }

    public void setStream(boolean _stream) {
        this._stream = _stream;
    }

    public boolean isTelemetry() {
        return _telemetry;
    }

    public void setTelemetry(boolean _telemetry) {
        this._telemetry = _telemetry;
    }
}