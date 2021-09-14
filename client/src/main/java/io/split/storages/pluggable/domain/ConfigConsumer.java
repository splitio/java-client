package io.split.storages.pluggable.domain;

import com.google.gson.annotations.SerializedName;
import io.split.telemetry.domain.Rates;
import io.split.telemetry.domain.URLOverrides;

import java.util.List;

public class ConfigConsumer {
    /* package private */ static final String FIELD_OPERATION_MODE = "oM";
    /* package private */ static final String FIELD_STORAGE = "st";
    /* package private */ static final String FIELD_ACTIVE_FACTORIES = "aF";
    /* package private */ static final String FIELD_REDUNDANT_FACTORIES = "rF";
    /* package private */ static final String FIELD__TAGS = "t";

    @SerializedName(FIELD_OPERATION_MODE)
    private int _operationMode;
    @SerializedName(FIELD_STORAGE)
    private String _storage;
    @SerializedName(FIELD_ACTIVE_FACTORIES)
    private long _activeFactories;
    @SerializedName(FIELD_REDUNDANT_FACTORIES)
    private long _redundantFactories;
    @SerializedName(FIELD__TAGS)
    private List<String> _tags;

    public int get_operationMode() {
        return _operationMode;
    }

    public void set_operationMode(int _operationMode) {
        this._operationMode = _operationMode;
    }

    public String get_storage() {
        return _storage;
    }

    public void set_storage(String _storage) {
        this._storage = _storage;
    }

    public long get_activeFactories() {
        return _activeFactories;
    }

    public void set_activeFactories(long _activeFactories) {
        this._activeFactories = _activeFactories;
    }

    public long get_redundantFactories() {
        return _redundantFactories;
    }

    public void set_redundantFactories(long _redundantFactories) {
        this._redundantFactories = _redundantFactories;
    }

    public List<String> get_tags() {
        return _tags;
    }

    public void set_tags(List<String> _tags) {
        this._tags = _tags;
    }
}
