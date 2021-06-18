package io.split.telemetry.domain;

import com.google.gson.annotations.SerializedName;

public class MethodExceptions {
    /* package private */ static final String FIELD_TREATMENT = "t";
    /* package private */ static final String FIELD_TREATMENTS = "ts";
    /* package private */ static final String FIELD_TREATMENT_WITH_CONFIG = "tc";
    /* package private */ static final String FIELD_TREATMENTS_WITH_CONFIG = "tcs";
    /* package private */ static final String FIELD_TRACK = "tr";

    @SerializedName(FIELD_TREATMENT)
    private long _treatment;
    @SerializedName(FIELD_TREATMENTS)
    private long _treatments;
    @SerializedName(FIELD_TREATMENT_WITH_CONFIG)
    private long _treatmentWithConfig;
    @SerializedName(FIELD_TREATMENTS_WITH_CONFIG)
    private long _treatmentsWithConfig;
    @SerializedName(FIELD_TRACK)
    private long _track;

    public long get_treatment() {
        return _treatment;
    }

    public void set_treatment(long _treatment) {
        this._treatment = _treatment;
    }

    public long get_treatments() {
        return _treatments;
    }

    public void set_treatments(long _treatments) {
        this._treatments = _treatments;
    }

    public long get_treatmentsWithConfig() {
        return _treatmentsWithConfig;
    }

    public void set_treatmentsWithConfig(long _treatmentsWithConfig) {
        this._treatmentsWithConfig = _treatmentsWithConfig;
    }

    public long get_treatmentWithConfig() {
        return _treatmentWithConfig;
    }

    public void set_treatmentWithConfig(long _treatmentWithConfig) {
        this._treatmentWithConfig = _treatmentWithConfig;
    }

    public long get_track() {
        return _track;
    }

    public void set_track(long _track) {
        this._track = _track;
    }
}
