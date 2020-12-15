package io.split.inputValidation;

public class InputValidationResult {
    private final boolean _success;
    private final String _value;

    public InputValidationResult(boolean success, String value) {
        _success = success;
        _value = value;
    }

    public InputValidationResult(boolean success) {
        _success = success;
        _value = null;
    }

    public boolean getSuccess() {
        return _success;
    }

    public String getValue() {
        return _value;
    }
}
