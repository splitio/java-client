package io.split.telemetry.domain;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class MethodLatencies {
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
    private List<Long> _treatment;
    @SerializedName(FIELD_TREATMENTS)
    private List<Long> _treatments;
    @SerializedName(FIELD_TREATMENT_WITH_CONFIG)
    private List<Long> _treatmentWithConfig;
    @SerializedName(FIELD_TREATMENTS_WITH_CONFIG)
    private List<Long> _treatmentsWithConfig;
    @SerializedName(FIELD_TREATMENT_BY_FLAG_SET)
    private List<Long> _treatmentByFlagSet;
    @SerializedName(FIELD_TREATMENT_BY_FLAG_SETS)
    private List<Long> _treatmentByFlagSets;
    @SerializedName(FIELD_TREATMENT_WITH_CONFIG_BY_FLAG_SET)
    private List<Long> _treatmentWithConfigByFlagSet;
    @SerializedName(FIELD_TREATMENT_WITH_CONFIG_BY_FLAG_SETS)
    private List<Long> _treatmentWithConfigByFlagSets;
    @SerializedName(FIELD_TRACK)
    private List<Long> _track;

    public MethodLatencies() {
        _treatment = new ArrayList<>();
        _treatments = new ArrayList<>();
        _treatmentWithConfig = new ArrayList<>();
        _treatmentsWithConfig = new ArrayList<>();
        _treatmentByFlagSet = new ArrayList<>();
        _treatmentByFlagSets = new ArrayList<>();
        _treatmentWithConfigByFlagSet = new ArrayList<>();
        _treatmentWithConfigByFlagSets = new ArrayList<>();
        _track = new ArrayList<>();
    }

    public List<Long> getTreatment() {
        return _treatment;
    }

    public void setTreatment(List<Long> treatment) {
        this._treatment = treatment;
    }

    public List<Long> getTreatments() {
        return _treatments;
    }

    public void setTreatments(List<Long> treatments) {
        this._treatments = treatments;
    }

    public List<Long> getTreatmentsWithConfig() {
        return _treatmentsWithConfig;
    }

    public void setTreatmentsWithConfig(List<Long> treatmentsWithConfig) {
        this._treatmentsWithConfig = treatmentsWithConfig;
    }

    public List<Long> getTreatmentWithConfig() {
        return _treatmentWithConfig;
    }

    public void setTreatmentWithConfig(List<Long> treatmentWithConfig) {
        this._treatmentWithConfig = treatmentWithConfig;
    }

    public List<Long> getTrack() {
        return _track;
    }

    public void setTrack(List<Long> track) {
        this._track = track;
    }

    public List<Long> getTreatmentByFlagSet() {
        return _treatmentByFlagSet;
    }

    public List<Long> getTreatmentByFlagSets() {
        return _treatmentByFlagSets;
    }

    public void setTreatmentByFlagSet(List<Long> treatmentByFlagSet) {
        this._treatmentByFlagSet = treatmentByFlagSet;
    }

    public void setTreatmentByFlagSets(List<Long> treatmentByFlagSets) {
        this._treatmentByFlagSets = treatmentByFlagSets;
    }

    public List<Long> getTreatmentWithConfigByFlagSet() {
        return _treatmentWithConfigByFlagSet;
    }

    public void setTreatmentWithConfigByFlagSet(List<Long> treatmentWithConfigByFlagSet) {
        this._treatmentWithConfigByFlagSet = treatmentWithConfigByFlagSet;
    }

    public void setTreatmentWithConfigByFlagSets(List<Long> treatmentWithConfigByFlagSets) {
        this._treatmentWithConfigByFlagSets = treatmentWithConfigByFlagSets;
    }

    public List<Long> getTreatmentWithConfigByFlagSets() {
        return _treatmentWithConfigByFlagSets;
    }
}