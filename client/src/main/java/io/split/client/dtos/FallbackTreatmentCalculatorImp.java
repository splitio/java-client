package io.split.client.dtos;

import io.split.grammar.Treatments;

public class FallbackTreatmentCalculatorImp implements FallbackTreatmentCalculator
{
    private final FallbackTreatmentsConfiguration _fallbackTreatmentsConfiguration;
    private final static String labelPrefix = "fallback - ";

    public FallbackTreatmentCalculatorImp(FallbackTreatmentsConfiguration fallbackTreatmentsConfiguration) {
        _fallbackTreatmentsConfiguration = fallbackTreatmentsConfiguration;
    }

    public FallbackTreatment resolve(String flagName, String label) {
        if (_fallbackTreatmentsConfiguration != null) {
            if (_fallbackTreatmentsConfiguration.getByFlagFallbackTreatment() != null
                && _fallbackTreatmentsConfiguration.getByFlagFallbackTreatment().get(flagName) != null) {
                return copyWithLabel(_fallbackTreatmentsConfiguration.getByFlagFallbackTreatment().get(flagName),
                        resolveLabel(label));
            }
            if (_fallbackTreatmentsConfiguration.getGlobalFallbackTreatment() != null) {
                return copyWithLabel(_fallbackTreatmentsConfiguration.getGlobalFallbackTreatment(),
                        resolveLabel(label));
            }
        }

        return new FallbackTreatment(Treatments.CONTROL, null, label);
    }

    private String resolveLabel(String label) {
        if (label == null) {
            return null;
        }
        return labelPrefix + label;
    }

    private FallbackTreatment copyWithLabel(FallbackTreatment fallbackTreatment, String label) {
        return new FallbackTreatment(fallbackTreatment.getTreatment(), fallbackTreatment.getConfig(), label);
    }
}
