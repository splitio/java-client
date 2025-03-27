package io.split.inputValidation;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.split.client.dtos.KeyImpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Maps.toMap;
import static java.util.stream.Collectors.toList;

public class ImpressionPropertiesValidator {
    private static final Logger _log = LoggerFactory.getLogger(ImpressionPropertiesValidator.class);

    public static ImpressionPropertiesValidatorResult propertiesAreValid(JsonObject properties) {
        int size = 1024; // We assume 1kb events without properties (750 bytes avg measured)

        if (properties == null) {
            return new ImpressionPropertiesValidatorResult(true);
        }
        Map<String, Object> propertiesMap = new Gson().fromJson(properties, Map.class);
        if (propertiesMap.size() > 300) {
            _log.warn("Impression properties has more than 300 properties. Some of them will be trimmed when processed");
        }

        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : propertiesMap.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isEmpty()) {
              continue;
            }

            size += entry.getKey().length();
            Object value = entry.getValue();

            if (!(value instanceof Number) && !(value instanceof Boolean) && !(value instanceof String)) {
                _log.warn(String.format("Property %s is of invalid type. Setting value to null", entry.getKey()));
                value = null;
            }

            if (value instanceof String) {
                size += ((String) value).length();
            }

            if (size > KeyImpression.MAX_PROPERTIES_LENGTH_BYTES) {
                _log.error(String.format("The maximum size allowed for the properties is 32768 bytes. "
                        + "Current one is %s bytes. Properties field is ignored", size));

                return new ImpressionPropertiesValidatorResult(false);
            }

            result.put(entry.getKey(), value);
        }

        return new ImpressionPropertiesValidatorResult(true, size, result);
    }

    public static class ImpressionPropertiesValidatorResult {
        private final boolean _success;
        private final int _propertySize;
        private final Map<String, Object> _value;

        public ImpressionPropertiesValidatorResult(boolean success, int propertySize, Map<String, Object> value) {
            _success = success;
            _propertySize = propertySize;
            _value = value;
        }

        public ImpressionPropertiesValidatorResult(boolean success) {
            _success = success;
            _propertySize = 0;
            _value = null;
        }

        public boolean getSuccess() {
            return _success;
        }

        public int getSize() {
            return _propertySize;
        }

        public Map<String, Object> getValue() {
            return _value;
        }
    }
}
