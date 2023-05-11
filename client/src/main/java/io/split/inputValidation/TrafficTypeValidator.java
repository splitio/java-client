package io.split.inputValidation;

import io.split.storages.SplitCacheConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class TrafficTypeValidator {
    private static final Logger _log = LoggerFactory.getLogger(TrafficTypeValidator.class);

    public static Optional<String> isValid(String trafficTypeName, SplitCacheConsumer splitCacheConsumer, String method) {
        if (trafficTypeName == null) {
            _log.error(String.format("%s: you passed a null trafficTypeName, trafficTypeName must be a non-empty string", method));
            return Optional.empty();
        }

        if (trafficTypeName.isEmpty()) {
            _log.error(String.format("%s: you passed an empty trafficTypeName, trafficTypeName must be a non-empty string", method));
            return Optional.empty();
        }

        if (!trafficTypeName.equals(trafficTypeName.toLowerCase())) {
            _log.warn(String.format("%s: trafficTypeName should be all lowercase - converting string to lowercase", method));
            trafficTypeName = trafficTypeName.toLowerCase();
        }

        if (!splitCacheConsumer.trafficTypeExists(trafficTypeName)) {
            _log.warn(String.format("%s: Traffic Type %s does not have any corresponding Feature flags in this environment, " +
                    "make sure you’re tracking your events to a valid traffic type defined in the Split user interface.", method, trafficTypeName));
        }

        return Optional.of(trafficTypeName);
    }
}
