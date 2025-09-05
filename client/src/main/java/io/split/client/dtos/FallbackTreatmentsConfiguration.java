package io.split.client.dtos;

import java.util.Map;

public class FallbackTreatmentsConfiguration {
    private final FallbackTreatment _globalFallbackTreatment;
    private final Map<String, FallbackTreatment> _byFlagFallbackTreatment;

    public FallbackTreatmentsConfiguration(FallbackTreatment globalFallbackTreatment, Map<String, FallbackTreatment> byFlagFallbackTreatment) {
        _globalFallbackTreatment = globalFallbackTreatment;
        _byFlagFallbackTreatment = byFlagFallbackTreatment;
    }

    public FallbackTreatment getGlobalFallbackTreatment() {
        return _globalFallbackTreatment;
    }
    public void setGlobalFallbackTreatment(FallbackTreatment newValue) {
         _globalFallbackTreatment = newValue;
    }

    public Map<String, FallbackTreatment> getByFlagFallbackTreatment() { return _byFlagFallbackTreatment;}
    public void setByFlagFallbackTreatment(Map<String, FallbackTreatment> newValue) {
        _byFlagFallbackTreatment = newValue;
    }
}
