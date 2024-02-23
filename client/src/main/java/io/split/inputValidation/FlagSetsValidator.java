package io.split.inputValidation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

public final class FlagSetsValidator {

    private static final String FLAG_SET_REGEX = "^[a-z0-9][_a-z0-9]{0,49}$";
    private static final Logger _log = LoggerFactory.getLogger(FlagSetsValidator.class);

    private FlagSetsValidator() {
        throw new IllegalStateException("Utility class");
    }

    public static Set<String> cleanup(List<String> flagSets) {
        TreeSet<String> cleanFlagSets = new TreeSet<>();
        if (flagSets == null || flagSets.isEmpty()) {
            return cleanFlagSets;
        }
        for (String flagSet: flagSets) {
            if(!flagSet.equals(flagSet.toLowerCase())) {
                _log.warn(String.format("Flag Set name %s should be all lowercase - converting string to lowercase", flagSet));
                flagSet = flagSet.toLowerCase();
            }
            if (!flagSet.equals(flagSet.trim())) {
                _log.warn(String.format("Flag Set name %s has extra whitespace, trimming", flagSet));
                flagSet = flagSet.trim();
            }
            if (!Pattern.matches(FLAG_SET_REGEX, flagSet)) {
                _log.warn(String.format("you passed %s, Flag Set must adhere to the regular expressions %s. This means a Flag Set must be " +
                        "start with a letter or number, be in lowercase, alphanumeric and have a max length of 50 characters. %s was discarded.",
                        flagSet, FLAG_SET_REGEX, flagSet));
                continue;
            }
            cleanFlagSets.add(flagSet);
        }
        return cleanFlagSets;
    }
}