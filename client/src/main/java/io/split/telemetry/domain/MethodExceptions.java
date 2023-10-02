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

    public long getTreatment() {
        return _treatment;
    }

    public void setTreatment(long treatment) {
        this._treatment = treatment;
    }

    public long getTreatments() {
        return _treatments;
    }

    public void setTreatments(long treatments) {
        this._treatments = treatments;
    }

    public long getTreatmentsWithConfig() {
        return _treatmentsWithConfig;
    }

    public void setTreatmentsWithConfig(long treatmentsWithConfig) {
        this._treatmentsWithConfig = treatmentsWithConfig;
    }

    public long getTreatmentWithConfig() {
        return _treatmentWithConfig;
    }

    public void setTreatmentWithConfig(long treatmentWithConfig) {
        this._treatmentWithConfig = treatmentWithConfig;
    }

    public long getTrack() {
        return _track;
    }

    public void setTrack(long track) {
        this._track = track;
    }

    public long getTreatmentByFlagSet() {
        return _treatmentByFlagSet;
    }

    public long getTreatmentByFlagSets() {
        return _treatmentByFlagSets;
    }

    public long getTreatmentWithConfigByFlagSet() {
        return _treatmentWithConfigByFlagSet;
    }

    public long getTreatmentWithConfigByFlagSets() {
        return _treatmentWithConfigByFlagSets;
    }

    public void setTreatmentByFlagSet(Long treatmentByFlagSet) {
        this._treatmentByFlagSet = treatmentByFlagSet;
    }

    public void setTreatmentByFlagSets(Long treatmentByFlagSets) {
        this._treatmentByFlagSets = treatmentByFlagSets;
    }

    public void setTreatmentWithConfigByFlagSet(Long treatmentWithConfigByFlagSet) {
        this._treatmentWithConfigByFlagSet = treatmentWithConfigByFlagSet;
    }

    public void setTreatmentWithConfigByFlagSets(Long treatmentWithConfigByFlagSets) {
        this._treatmentWithConfigByFlagSets = treatmentWithConfigByFlagSets;
    }
}