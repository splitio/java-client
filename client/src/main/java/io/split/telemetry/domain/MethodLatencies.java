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

    public List<Long> get_treatment() {
        return _treatment;
    }

    public void set_treatment(List<Long> _treatment) {
        this._treatment = _treatment;
    }

    public List<Long> get_treatments() {
        return _treatments;
    }

    public void set_treatments(List<Long> _treatments) {
        this._treatments = _treatments;
    }

    public List<Long> get_treatmentsWithConfig() {
        return _treatmentsWithConfig;
    }

    public void set_treatmentsWithConfig(List<Long> _treatmentsWithConfig) {
        this._treatmentsWithConfig = _treatmentsWithConfig;
    }

    public List<Long> get_treatmentWithConfig() {
        return _treatmentWithConfig;
    }

    public void set_treatmentWithConfig(List<Long> _treatmentWithConfig) {
        this._treatmentWithConfig = _treatmentWithConfig;
    }

    public List<Long> get_track() {
        return _track;
    }

    public void set_track(List<Long> _track) {
        this._track = _track;
    }

    public List<Long> get_treatmentByFlagSet() {
        return _treatmentByFlagSet;
    }

    public List<Long> get_treatmentByFlagSets() {
        return _treatmentByFlagSets;
    }

    public void set_treatmentByFlagSet(List<Long> _treatmentByFlagSet) {
        this._treatmentByFlagSet = _treatmentByFlagSet;
    }

    public void set_treatmentByFlagSets(List<Long> _treatmentByFlagSets) {
        this._treatmentByFlagSets = _treatmentByFlagSets;
    }

    public List<Long> get_treatmentWithConfigByFlagSet() {
        return _treatmentWithConfigByFlagSet;
    }

    public void set_treatmentWithConfigByFlagSet(List<Long> _treatmentWithConfigByFlagSet) {
        this._treatmentWithConfigByFlagSet = _treatmentWithConfigByFlagSet;
    }

    public void set_treatmentWithConfigByFlagSets(List<Long> _treatmentWithConfigByFlagSets) {
        this._treatmentWithConfigByFlagSets = _treatmentWithConfigByFlagSets;
    }

    public List<Long> get_treatmentWithConfigByFlagSets() {
        return _treatmentWithConfigByFlagSets;
    }
}