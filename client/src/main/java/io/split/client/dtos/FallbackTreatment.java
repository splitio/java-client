package io.split.client.dtos;

import java.util.Map;

public class FallbackTreatment {
    private final Map<String, Object> _config;
    private final String _treatment;
    private final String _label;

    public FallbackTreatment(String treatment, Map<String, Object> config) {
        _treatment = treatment;
        _config = config;
        _label = "fallback - ";
    }

    public FallbackTreatment(String treatment) {
        _treatment = treatment;
        _config = null;
        _label = "fallback - ";
    }

    public Map<String, Object> getConfig() {
        return _config;
    }

    public String getTreatment() {
        return _treatment;
    }

    public String getLabel() {
        return _label;
    }
}
