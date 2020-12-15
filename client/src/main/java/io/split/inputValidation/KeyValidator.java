package io.split.inputValidation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeyValidator {
    private static final Logger _log = LoggerFactory.getLogger(KeyValidator.class);

    public static boolean isValid(String key, String propertyName, int maxStringLength, String method) {
        if (key == null) {
            _log.error(String.format("%s: you passed a null %s, %s must be a non-empty string", method, propertyName, propertyName));
            return false;
        }

        if (key.isEmpty()) {
            _log.error(String.format("%s: you passed an empty %s, %s must be a non-empty string", method, propertyName, propertyName));
            return false;
        }

        if (key.length() > maxStringLength) {
            _log.error(String.format("%s: %s too long - must be %s characters or less", method, propertyName, maxStringLength));
            return false;
        }

        return true;
    }

    public static boolean bucketingKeyIsValid(String bucketingKey, int maxStringLength, String method) {
        if (bucketingKey != null && bucketingKey.isEmpty()) {
            _log.error(String.format("%s: you passed an empty string, %s must be a non-empty string", method, "bucketingKey"));
            return false;
        }

        if (bucketingKey != null && bucketingKey.length() > maxStringLength) {
            _log.error(String.format("%s: bucketingKey too long - must be %s characters or less", method, maxStringLength));
            return false;
        }

        return true;
    }
}
