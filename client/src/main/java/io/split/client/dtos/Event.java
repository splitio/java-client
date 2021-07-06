package io.split.client.dtos;

import com.google.common.base.Objects;
import com.google.gson.annotations.SerializedName;

import java.util.Map;

public class Event {

    /* package private */ static final String FIELD_EVENT_TYPE_ID = "eventTypeId";
    /* package private */ static final String FIELD_TRAFFIC_TYPE_NAME = "trafficTypeName";
    /* package private */ static final String FIELD_KEY = "key";
    /* package private */ static final String FIELD_VALUE = "value";
    /* package private */ static final String FIELD_PROPERTIES = "properties";
    /* package private */ static final String FIELD_TIMESTAMP = "timestamp";

    public static int MAX_PROPERTIES_LENGTH_BYTES = 32 * 1024;

    @SerializedName(FIELD_EVENT_TYPE_ID)
    public String eventTypeId;
    @SerializedName(FIELD_TRAFFIC_TYPE_NAME)
    public String trafficTypeName;
    @SerializedName(FIELD_KEY)
    public String key;
    @SerializedName(FIELD_VALUE)
    public double value;
    @SerializedName(FIELD_PROPERTIES)
    public Map<String, Object> properties;
    @SerializedName(FIELD_TIMESTAMP)
    public long timestamp;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Event event = (Event) o;
        return Double.compare(event.value, value) == 0 &&
                timestamp == event.timestamp &&
                Objects.equal(eventTypeId, event.eventTypeId) &&
                Objects.equal(trafficTypeName, event.trafficTypeName) &&
                Objects.equal(key, event.key);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(eventTypeId, trafficTypeName, key, value, timestamp);
    }
}
