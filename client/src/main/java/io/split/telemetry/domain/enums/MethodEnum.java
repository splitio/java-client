package io.split.telemetry.domain.enums;

public enum MethodEnum {
    TREATMENT("getTreatment"),
    TREATMENTS("getTreatments"),
    TREATMENT_WITH_CONFIG("getTreatmentWithConfig"),
    TREATMENTS_WITH_CONFIG("getTreatmentsWithConfig"),
    TRACK("track");

    private String _method;

    MethodEnum(String method) {
        _method = method;
    }

    public String getMethod() {
        return _method;
    }
}
