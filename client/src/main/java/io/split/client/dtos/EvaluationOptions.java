package io.split.client.dtos;

import java.util.Map;

public class EvaluationOptions {
    private Map<String, Object> _properties;

    public EvaluationOptions(Map<String, Object> properties) {
        _properties = properties;
    }
    public Map<String, Object> getProperties() {
        return _properties;
    }
}
