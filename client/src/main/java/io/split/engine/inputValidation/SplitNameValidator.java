package io.split.engine.inputValidation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SplitNameValidator {
    private static final Logger _log = LoggerFactory.getLogger(SplitNameValidator.class);

    public static SplitNameResult isValid(String name, String method) {
        if (name == null) {
            _log.error(String.format("%s: you passed a null split name, split name must be a non-empty string", method));
            return new SplitNameResult(false);
        }

        if (name.isEmpty()) {
            _log.error(String.format("%s: you passed an empty split name, split name must be a non-empty string", method));
            return new SplitNameResult(false);
        }

        String trimmed = name.trim();
        if (!trimmed.equals(name)) {
            _log.warn(String.format("%s: split name %s has extra whitespace, trimming", method));
            name = trimmed;
        }

        return new SplitNameResult(true, name);
    }

    public static class SplitNameResult {
        private final boolean _success;
        private final String _value;

        public SplitNameResult(boolean success) {
            _success = success;
            _value = null;
        }

        public SplitNameResult(boolean success, String value) {
            _success = success;
            _value = value;
        }

        public boolean getSuccess() {
            return _success;
        }

        public String getValue() {
            return _value;
        }
    }
}
