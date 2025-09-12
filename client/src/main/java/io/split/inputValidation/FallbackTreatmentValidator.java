package io.split.inputValidation;

import io.split.client.dtos.FallbackTreatment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import static io.split.inputValidation.SplitNameValidator.isValid;

public class FallbackTreatmentValidator {
    private static final Logger _log = LoggerFactory.getLogger(FallbackTreatmentValidator.class);
    private static final Pattern TREATMENT_MATCHER = Pattern.compile("^[0-9]+[.a-zA-Z0-9_-]*$|^[a-zA-Z]+[a-zA-Z0-9_-]*$");
    private static final int MAX_LENGTH = 100;

    public static String isValidTreatment(String name, String method) {
        if (name == null) {
            _log.error(String.format("%s: you passed a null treatment, fallback treatment must be a non-empty string", method));
            return null;
        }

        if (name.isEmpty()) {
            _log.error(String.format("%s: you passed an empty treatment, fallback treatment must be a non-empty string", method));
            return null;
        }

        String trimmed = name.trim();
        if (!trimmed.equals(name)) {
            _log.warn(String.format("%s: fallback treatment %s has extra whitespace, trimming", method, name));
            name = trimmed;
        }

        if (name.length() > MAX_LENGTH) {
            return null;
        }

        if (!TREATMENT_MATCHER.matcher(name).find()) {
            _log.error(String.format("%s: you passed %s, treatment must adhere to the regular expression " +
                    "^[0-9]+[.a-zA-Z0-9_-]*$|^[a-zA-Z]+[a-zA-Z0-9_-]*$", method, name));
            return null;
        }

        return name;
    }

    public static Map<String, FallbackTreatment> isValidByFlagTreatment(Map<String, FallbackTreatment> byFlagTreatment, String method) {
        Map<String, FallbackTreatment> result = new HashMap<>();
        for (Map.Entry<String, FallbackTreatment> entry : byFlagTreatment.entrySet()) {
            Optional<String> featureName = isValid(entry.getKey(), method);
            if (featureName.equals(Optional.empty())) {
                continue;
            }

            FallbackTreatment fallbackTreatment = entry.getValue();
            String treatment = isValidTreatment(fallbackTreatment.getTreatment(), method);
            if (treatment != null) {
                result.put(featureName.get(), new FallbackTreatment(treatment, fallbackTreatment.getConfig()));
            }
        }

        return result;
    }
}
