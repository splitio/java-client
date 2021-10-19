package io.split.storages.pluggable.domain;

import com.google.gson.annotations.SerializedName;
import io.split.client.dtos.KeyImpression;
import io.split.client.dtos.Metadata;

public class ImpressionConsumer {
    /* package private */ static final String FIELD_METADATA = "m";
    /* package private */ static final String FIELD_KEY_IMPRESSION = "i";

    @SerializedName(FIELD_METADATA)
    private Metadata _metadata;
    @SerializedName(FIELD_KEY_IMPRESSION)
    private ImpressionDto _impressionDto;

    public ImpressionConsumer(Metadata metadata, KeyImpression keyImpression) {
        _metadata = metadata;
        _impressionDto = ImpressionDto.fromKeyImpression(keyImpression);
    }

    public Metadata getMetadata() {
        return _metadata;
    }

    public ImpressionDto getKeyImpression() {
        return _impressionDto;
    }
}
