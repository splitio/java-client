package io.split.client.dtos.pluggable;

import com.google.gson.annotations.SerializedName;
import io.split.client.dtos.Event;
import io.split.client.dtos.Metadata;

public class EventsPluggable extends Event {

    /* package private */ static final String FIELD_METADATA = "m";
    @SerializedName(FIELD_METADATA)
    private final Metadata _metadata;

    public EventsPluggable(Metadata metadata) {
        super();
        _metadata = metadata;
    }

    public Metadata getMetadata() {
        return _metadata;
    }
}
