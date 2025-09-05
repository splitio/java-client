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

    public Map<String, FallbackTreatment> getByFlagFallbackTreatment() { return _byFlagFallbackTreatment;}
}
