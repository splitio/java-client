package io.split.storages.pluggable.domain;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ConfigConsumer {
    /* package private */ static final String FIELD_OPERATION_MODE = "oM";
    /* package private */ static final String FIELD_STORAGE = "st";
    /* package private */ static final String FIELD_ACTIVE_FACTORIES = "aF";
    /* package private */ static final String FIELD_REDUNDANT_FACTORIES = "rF";
    /* package private */ static final String FIELD_TAGS = "t";
    /* package private */ static final String FIELD_FLAG_SETS_TOTAL = "fsT";
    /* package private */ static final String FIELD_FLAG_SETS_INVALID = "fsI";

    @SerializedName(FIELD_OPERATION_MODE)
    private int _operationMode;
    @SerializedName(FIELD_STORAGE)
    private String _storage;
    @SerializedName(FIELD_ACTIVE_FACTORIES)
    private long _activeFactories;
    @SerializedName(FIELD_REDUNDANT_FACTORIES)
    private long _redundantFactories;
    @SerializedName(FIELD_TAGS)
    private List<String> _tags;
    @SerializedName(FIELD_FLAG_SETS_TOTAL)
    private int _flagSetsTotal;
    @SerializedName(FIELD_FLAG_SETS_INVALID)
    private int _flagSetsInvalid;

    public int getOperationMode() {
        return _operationMode;
    }

    public void setOperationMode(int operationMode) {
        this._operationMode = operationMode;
    }

    public String getStorage() {
        return _storage;
    }

    public void setStorage(String storage) {
        this._storage = storage;
    }

    public long getActiveFactories() {
        return _activeFactories;
    }

    public void setActiveFactories(long activeFactories) {
        this._activeFactories = activeFactories;
    }

    public long getRedundantFactories() {
        return _redundantFactories;
    }

    public void setRedundantFactories(long redundantFactories) {
        this._redundantFactories = redundantFactories;
    }

    public List<String> getTags() {
        return _tags;
    }

    public void setTags(List<String> tags) {
        this._tags = tags;
    }

    public int getFlagSetsTotal() {
        return _flagSetsTotal;
    }

    public void setFlagSetsTotal(int flagSetsTotal) {
        this._flagSetsTotal = flagSetsTotal;
    }

    public int getFlagSetsInvalid() {
        return _flagSetsInvalid;
    }

    public void setFlagSetsInvalid(int flagSetsInvalid) {
        this._flagSetsInvalid = flagSetsInvalid;
    }
}