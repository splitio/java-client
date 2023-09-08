package io.split.client.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class FlagSetSanitizer {

    private static final String FLAG_SET_REGEX = "^[a-z0-9][_a-z0-9]{0,49}$";
    private static final Logger _log = LoggerFactory.getLogger(FlagSetSanitizer.class);

    private FlagSetSanitizer() {
        throw new IllegalStateException("Utility class");
    }

    public static List<String> sanitizeFlagSet(List<String> flagSets) {
        if (flagSets == null || flagSets.isEmpty()) {
            _log.error("FlagSets must be a non-empty list.");
            return new ArrayList<>();
        }
        HashSet<String> sanitizedFlagSets = new HashSet<>();
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
                _log.warn(String.format("you passed %s, Flag Set must adhere to the regular expressions %s. This means an Flag Set must be " +
                        "start with a letter, be in lowercase, alphanumeric and have a max length of 50 characters. %s was discarded.",
                        flagSet, FLAG_SET_REGEX, flagSet));
                continue;
            }
            sanitizedFlagSets.add(flagSet);
        }
        return sanitizedFlagSets.stream().sorted().collect(Collectors.toList());
    }
}