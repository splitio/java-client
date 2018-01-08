package io.split.client.dtos;

import com.google.common.base.Objects;

public class Event {

    public String eventTypeId;
    public String trafficTypeName;
    public String key;
    public double value;
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
