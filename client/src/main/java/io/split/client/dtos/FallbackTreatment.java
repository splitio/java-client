package io.split.client.dtos;

public class FallbackTreatment {
    private final String _config;
    private final String _treatment;
    private final String _label;

    public FallbackTreatment(String treatment, String config) {
        _treatment = treatment;
        _config = config;
        _label = null;
    }

    public FallbackTreatment(String treatment) {
        _treatment = treatment;
        _config = null;
        _label = null;
    }

    public FallbackTreatment(String treatment, String config, String label) {
        _treatment = treatment;
        _config = config;
        _label = label;
    }

    public String getConfig() {
        return _config;
    }

    public String getTreatment() {
        return _treatment;
    }

    public String getLabel() {
        return _label;
    }
}
