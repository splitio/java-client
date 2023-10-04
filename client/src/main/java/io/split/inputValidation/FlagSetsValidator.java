package io.split.inputValidation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

public final class FlagSetsValidator {

    private static final String FLAG_SET_REGEX = "^[a-z0-9][_a-z0-9]{0,49}$";
    private static final Logger _log = LoggerFactory.getLogger(FlagSetsValidator.class);

    private FlagSetsValidator() {
        throw new IllegalStateException("Utility class");
    }

    public static FSValidatorResult cleanup(List<String> flagSets) {
        if (flagSets == null || flagSets.isEmpty()) {
            return new FSValidatorResult(new HashSet<>(), 0);
        }
        HashSet<String> cleanFlagSets = new HashSet<>();
        int invalidSets = 0;
        for (String flagSet: flagSets) {
            if(flagSet != flagSet.toLowerCase()) {
                _log.warn(String.format("Flag Set name %s should be all lowercase - converting string to lowercase", flagSet));
                flagSet = flagSet.toLowerCase();
            }
            if (flagSet.trim() != flagSet) {
                _log.warn(String.format("Flag Set name %s has extra whitespace, trimming", flagSet));
                flagSet = flagSet.trim();
            }
            if (!Pattern.matches(FLAG_SET_REGEX, flagSet)) {
                invalidSets ++;
                _log.warn(String.format("you passed %s, Flag Set must adhere to the regular expressions %s. This means an Flag Set must be " +
                        "start with a letter, be in lowercase, alphanumeric and have a max length of 50 characters. %s was discarded.",
                        flagSet, FLAG_SET_REGEX, flagSet));
                continue;
            }
            cleanFlagSets.add(flagSet);
        }
        return new FSValidatorResult(cleanFlagSets, invalidSets);
    }

    public static FlagSetsValidResult areValid(List<String> flagSets) {
        FSValidatorResult fsValidatorResult = cleanup(flagSets);
        HashSet<String> cleanFlagSets = fsValidatorResult.getFlagSets();
        return new FlagSetsValidResult(cleanFlagSets != null && cleanFlagSets.size() != 0, cleanFlagSets);
    }
}