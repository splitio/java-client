package io.split.telemetry.domain.enums;

public enum MethodEnum {
    TREATMENT("getTreatment"),
    TREATMENTS("getTreatments"),
    TREATMENT_WITH_CONFIG("getTreatmentWithConfig"),
    TREATMENTS_WITH_CONFIG("getTreatmentsWithConfig"),
    TREATMENTS_BY_FLAG_SET("getTreatmentsByFlagSet"),
    TREATMENTS_BY_FLAG_SETS("getTreatmentsByFlagSets"),
    TREATMENTS_WITH_CONFIG_BY_FLAG_SET("getTreatmentsWithConfigByFlagSet"),
    TREATMENTS_WITH_CONFIG_BY_FLAG_SETS("getTreatmentsWithConfigByFlagSets"),
    TRACK("track");

    private String _method;

    MethodEnum(String method) {
        _method = method;
    }

    public String getMethod() {
        return _method;
    }
}
