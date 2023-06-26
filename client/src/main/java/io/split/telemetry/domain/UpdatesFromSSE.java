package io.split.telemetry.domain;

import com.google.gson.annotations.SerializedName;

public class UpdatesFromSSE {

    /* package private */ static final String FIELD_FEATURE_FLAGS = "sp";

    @SerializedName(FIELD_FEATURE_FLAGS)
    private long splits;

    public long getSplits() {
        return splits;
    }

    public void setSplits(long splits) {
        this.splits = splits;
    }
}