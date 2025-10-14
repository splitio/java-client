package io.split.client.dtos;

import java.util.HashMap;
import java.util.Map;

public class FallbackTreatmentsConfiguration {
    private final FallbackTreatment _globalFallbackTreatment;
    private final Map<String, FallbackTreatment> _byFlagFallbackTreatment;

    public FallbackTreatmentsConfiguration(FallbackTreatment globalFallbackTreatment, Map<String, FallbackTreatment> byFlagFallbackTreatment) {
        _globalFallbackTreatment = globalFallbackTreatment;
        _byFlagFallbackTreatment = byFlagFallbackTreatment;
    }

    public FallbackTreatmentsConfiguration(Map<String, FallbackTreatment> byFlagFallbackTreatment) {
        _globalFallbackTreatment = null;
        _byFlagFallbackTreatment = byFlagFallbackTreatment;
    }

    public FallbackTreatmentsConfiguration(HashMap<String, String> byFlagFallbackTreatment) {
        _globalFallbackTreatment = null;
        _byFlagFallbackTreatment = buildByFlagFallbackTreatment(byFlagFallbackTreatment);
    }

    public FallbackTreatmentsConfiguration(FallbackTreatment globalFallbackTreatment) {
        _globalFallbackTreatment = globalFallbackTreatment;
        _byFlagFallbackTreatment = null;
    }

    public FallbackTreatmentsConfiguration(String globalFallbackTreatment, Map<String, FallbackTreatment> byFlagFallbackTreatment) {
        _globalFallbackTreatment = new FallbackTreatment(globalFallbackTreatment);
        _byFlagFallbackTreatment = byFlagFallbackTreatment;
    }

    public FallbackTreatmentsConfiguration(String globalFallbackTreatment) {
        _globalFallbackTreatment = new FallbackTreatment(globalFallbackTreatment);
        _byFlagFallbackTreatment = null;
    }


    public FallbackTreatmentsConfiguration(String globalFallbackTreatment, HashMap<String, String> byFlagFallbackTreatment) {
        _globalFallbackTreatment = new FallbackTreatment(globalFallbackTreatment);
        _byFlagFallbackTreatment = buildByFlagFallbackTreatment(byFlagFallbackTreatment);
    }

    public FallbackTreatmentsConfiguration(FallbackTreatment globalFallbackTreatment, HashMap<String, String> byFlagFallbackTreatment) {
        _globalFallbackTreatment = globalFallbackTreatment;
        _byFlagFallbackTreatment = buildByFlagFallbackTreatment(byFlagFallbackTreatment);
    }

    public FallbackTreatment getGlobalFallbackTreatment() {
        return _globalFallbackTreatment;
    }

    public Map<String, FallbackTreatment> getByFlagFallbackTreatment() { return _byFlagFallbackTreatment;}

    private Map<String, FallbackTreatment> buildByFlagFallbackTreatment(Map<String, String> byFlagFallbackTreatment) {
        Map<String, FallbackTreatment> result = new HashMap<>();
        for (Map.Entry<String, String> entry : byFlagFallbackTreatment.entrySet()) {
            result.put(entry.getKey(), new FallbackTreatment(entry.getValue()));
        }

        return result;
    }
}
