package io.split.telemetry.domain;

import com.google.gson.annotations.SerializedName;

public class MethodExceptions {
    /* package private */ static final String FIELD_TREATMENT = "t";
    /* package private */ static final String FIELD_TREATMENTS = "ts";
    /* package private */ static final String FIELD_TREATMENT_WITH_CONFIG = "tc";
    /* package private */ static final String FIELD_TREATMENTS_WITH_CONFIG = "tcs";
    /* package private */ static final String FIELD_TREATMENT_BY_FLAG_SET = "tf";
    /* package private */static final String FIELD_TREATMENT_BY_FLAG_SETS = "tfs";
    /* package private */static final String FIELD_TREATMENT_WITH_CONFIG_BY_FLAG_SET = "tcf";
    /* package private */static final String FIELD_TREATMENT_WITH_CONFIG_BY_FLAG_SETS = "tcfs";
    /* package private */ static final String FIELD_TRACK = "tr";

    @SerializedName(FIELD_TREATMENT)
    private long _treatment;
    @SerializedName(FIELD_TREATMENTS)
    private long _treatments;
    @SerializedName(FIELD_TREATMENT_WITH_CONFIG)
    private long _treatmentWithConfig;
    @SerializedName(FIELD_TREATMENTS_WITH_CONFIG)
    private long _treatmentsWithConfig;
    @SerializedName(FIELD_TREATMENT_BY_FLAG_SET)
    private Long _treatmentByFlagSet;
    @SerializedName(FIELD_TREATMENT_BY_FLAG_SETS)
    private Long _treatmentByFlagSets;
    @SerializedName(FIELD_TREATMENT_WITH_CONFIG_BY_FLAG_SET)
    private Long _treatmentWithConfigByFlagSet;
    @SerializedName(FIELD_TREATMENT_WITH_CONFIG_BY_FLAG_SETS)
    private Long _treatmentWithConfigByFlagSets;
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

    public long get_treatmentByFlagSet() {
        return _treatmentByFlagSet;
    }

    public long get_treatmentByFlagSets() {
        return _treatmentByFlagSets;
    }

    public long get_treatmentWithConfigByFlagSet() {
        return _treatmentWithConfigByFlagSet;
    }

    public long get_treatmentWithConfigByFlagSets() {
        return _treatmentWithConfigByFlagSets;
    }

    public void set_treatmentByFlagSet(Long _treatmentByFlagSet) {
        this._treatmentByFlagSet = _treatmentByFlagSet;
    }

    public void set_treatmentByFlagSets(Long _treatmentByFlagSets) {
        this._treatmentByFlagSets = _treatmentByFlagSets;
    }

    public void set_treatmentWithConfigByFlagSet(Long _treatmentWithConfigByFlagSet) {
        this._treatmentWithConfigByFlagSet = _treatmentWithConfigByFlagSet;
    }

    public void set_treatmentWithConfigByFlagSets(Long _treatmentWithConfigByFlagSets) {
        this._treatmentWithConfigByFlagSets = _treatmentWithConfigByFlagSets;
    }
}
