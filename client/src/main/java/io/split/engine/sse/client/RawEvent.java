package io.split.engine.sse.client;

import java.io.BufferedReader;
import java.io.StringReader;

public class RawEvent {

    private static final String FIELD_ID = "id";
    private static final String FIELD_EVENT = "event";
    private static final String FIELD_DATA = "data";
    private static final String FIELD_CLIENT_ID = "clientId";
    private static final String KV_DELIMITER = ":";

    private final String _id;
    private final String _event;
    private final String _data;
    private final String _clientId;

    public String id() { return _id; }
    public String event() { return _event; }
    public String data() { return _data; }
    public String clientId() { return _clientId; }

    private RawEvent(String id, String event, String data, String clientId) {
        _id = id;
        _event = event;
        _data = data;
        _clientId = clientId;
    }

    static class Builder {
        private String _id;
        private String _event;
        private String _data;
        private String _clientId;

        public Builder id(String i) { _id = i;  return this; }
        public Builder event(String e) { _event = e;  return this; }
        public Builder data(String d) { _data = d; return this; }
        public Builder clientId(String c) { _clientId = c; return this; }
        public RawEvent build() {
            return new RawEvent(_id, _event, _data, _clientId);
        }
    }

    public static RawEvent fromString(String eventRawData) {
        Builder eventBuilder = new Builder();
        BufferedReader fieldReader = new BufferedReader(new StringReader(eventRawData));
        fieldReader.lines().forEach(line -> {
            String[] parts = line.split(KV_DELIMITER, 2);
            if (parts.length < 2) {
                return;
            }
            switch (parts[0].trim()) {
                case FIELD_ID: eventBuilder.id(parts[1].trim()); break;
                case FIELD_CLIENT_ID: eventBuilder.clientId(parts[1].trim()); break;
                case FIELD_DATA: eventBuilder.data(parts[1].trim()); break;
                case FIELD_EVENT: eventBuilder.event(parts[1].trim()); break;
            }
        });
        return eventBuilder.build();
    }

    @Override
    public String toString() {
        return String.format("id=%s,clientId=%s,event=%s,data=%s", _id, _clientId, _event, _data);
    }
}