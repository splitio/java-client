package io.split.client.dtos.pluggable;

import com.google.gson.annotations.SerializedName;
import io.split.client.dtos.Metadata;
import io.split.telemetry.domain.Config;

public class ConfigPluggable extends Config {

    /* package private */ static final String FIELD_METADATA = "m";

    @SerializedName(FIELD_METADATA)
    private final Metadata _metadata;

    public ConfigPluggable(Metadata metadata) {
        super();
        _metadata = metadata;
    }

    public Metadata getMetadata() {
        return _metadata;
    }
}
