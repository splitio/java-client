package io.split.inputValidation;

import java.util.Map;

public class ImpressionPropertiesValidator {
    private ImpressionPropertiesValidator() {
        throw new IllegalStateException("Utility class");
    }
    public static ImpressionPropertiesValidatorResult propertiesAreValid(Map<String, Object> properties) {
        EventsValidator.EventValidatorResult result = EventsValidator.propertiesAreValid(properties);
        return new ImpressionPropertiesValidatorResult(result.getSuccess(), result.getEventSize(), result.getValue());
    }

    public static class ImpressionPropertiesValidatorResult extends EventsValidator.EventValidatorResult {
        public ImpressionPropertiesValidatorResult(boolean success, int eventSize, Map<String, Object> value) {
            super(success, eventSize, value);
        }
    }
}

