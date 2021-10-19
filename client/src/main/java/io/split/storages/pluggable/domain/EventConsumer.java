package io.split.storages.pluggable.domain;

import com.google.gson.annotations.SerializedName;
import io.split.client.dtos.Event;
import io.split.client.dtos.Metadata;

public class EventConsumer {

    /* package private */ static final String FIELD_METADATA = "m";
    /* package private */ static final String FIELD_KEY_IMPRESSION = "e";

    @SerializedName(FIELD_METADATA)
    private Metadata _metadata;
    @SerializedName(FIELD_KEY_IMPRESSION)
    private Event _event;

    public EventConsumer(Metadata metadata, Event event) {
        _metadata = metadata;
        _event = event;
    }

    public Metadata getMetadata() {
        return _metadata;
    }

    public Event getEventDto() {
        return _event;
    }
}
