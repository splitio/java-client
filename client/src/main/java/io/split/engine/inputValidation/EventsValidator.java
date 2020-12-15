package io.split.engine.inputValidation;

import io.split.client.dtos.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class EventsValidator {
    private static final Logger _log = LoggerFactory.getLogger(EventsValidator.class);
    public static final Pattern EVENT_TYPE_MATCHER = Pattern.compile("^[a-zA-Z0-9][-_.:a-zA-Z0-9]{0,79}$");

    public static EventValidatorResult propertiesAreValid(Map<String, Object> properties) {
        int size = 1024; // We assume 1kb events without properties (750 bytes avg measured)

        if (properties == null) {
            return new EventValidatorResult(true);
        }

        if (properties.size() > 300) {
            _log.warn("Event has more than 300 properties. Some of them will be trimmed when processed");
        }

        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            size += entry.getKey().length();
            Object value = entry.getValue();

            if (!(value instanceof Number) && !(value instanceof Boolean) && !(value instanceof String)) {
                _log.warn(String.format("Property %s is of invalid type. Setting value to null", entry.getKey()));
                value = null;
            }

            if (value instanceof String) {
                size += ((String) value).length();
            }

            if (size > Event.MAX_PROPERTIES_LENGTH_BYTES) {
                _log.error(String.format("The maximum size allowed for the properties is 32768 bytes. "
                        + "Current one is %s bytes. Event not queued", size));

                return new EventValidatorResult(false);
            }

            result.put(entry.getKey(), value);
        }

        return new EventValidatorResult(true, size, result);
    }

    public static boolean typeIsValid(String eventTypeId, String method) {
        if (eventTypeId == null) {
            _log.error(String.format("%s: you passed a null eventTypeId, eventTypeId must be a non-empty string", method));
            return false;
        }

        if (eventTypeId.isEmpty()) {
            _log.error(String.format("%s: you passed an empty eventTypeId, eventTypeId must be a non-empty string", method));
            return false;
        }

        if (!EVENT_TYPE_MATCHER.matcher(eventTypeId).find()) {
            _log.error(String.format("%s: you passed %s, eventTypeId must adhere to the regular expression " +
                    "[a-zA-Z0-9][-_.:a-zA-Z0-9]{0,79}. This means an eventTypeID must be alphanumeric, " +
                    "cannot be more than 80 characters long, and can only include a dash, underscore, period, " +
                    "or colon as separators of alphanumeric characters", method, eventTypeId));
            return false;
        }

        return true;
    }



    public static class EventValidatorResult {
        private final boolean _success;
        private final int _eventSize;
        private final Map<String, Object> _value;

        public EventValidatorResult(boolean success, int eventSize, Map<String, Object> value) {
            _success = success;
            _eventSize = eventSize;
            _value = value;
        }

        public EventValidatorResult(boolean success) {
            _success = success;
            _eventSize = 0;
            _value = null;
        }

        public boolean getSuccess() {
            return _success;
        }

        public int getEventSize() {
            return _eventSize;
        }

        public Map<String, Object> getValue() {
            return _value;
        }
    }
}
