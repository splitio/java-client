package io.split.client.utils;

import io.split.engine.common.Synchronizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

public final class FlagSetSanitizer {

    private final String FLAG_SET_REGEX = "^[a-z0-9][_a-z0-9]{0,49}$";
    private static final Logger _log = LoggerFactory.getLogger(FlagSetSanitizer.class);

    private FlagSetSanitizer() {
        throw new IllegalStateException("Utility class");
    }

    public HashSet<String> sanitizeFlagSet(List<String> flagSets) {
        if (flagSets == null || flagSets.isEmpty()) {
            _log.error("FlagSets must be a non-empty list.");
            return new HashSet<>();
        }
        HashSet<String> result = new HashSet<>();
        for (String flagSet: flagSets) {
            if(flagSet != flagSet.toLowerCase()) {
                _log.warn(String.format("Flag Set name %s should be all lowercase - converting string to lowercase", flagSet));
                flagSet = flagSet.toLowerCase();
            }
            if (flagSet.trim() != flagSet) {
                _log.warn(String.format("Flag Set name %s has extra whitespace, trimming", flagSet));
                flagSet = flagSet.trim();
            }
            if ()
        }

    }
}
