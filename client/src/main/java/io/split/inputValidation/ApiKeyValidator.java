package io.split.inputValidation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApiKeyValidator {
    private static final Logger _log = LoggerFactory.getLogger(ApiKeyValidator.class);

    public static void validate(String apiToken) {
        if (apiToken == null) {
            _log.error("factory instantiation: you passed a null apiToken, apiToken must be a non-empty string");
        }
        if (apiToken.isEmpty()) {
            _log.error("factory instantiation: you passed and empty apiToken, apiToken be a non-empty string");
        }
    }
}
