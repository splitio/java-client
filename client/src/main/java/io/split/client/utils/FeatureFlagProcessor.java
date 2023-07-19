package io.split.client.utils;

import io.split.client.dtos.Split;
import io.split.client.dtos.Status;
import io.split.engine.experiments.ParsedSplit;
import io.split.engine.experiments.SplitParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FeatureFlagProcessor {
    private static final Logger _log = LoggerFactory.getLogger(FeatureFlagProcessor.class);

    public static FeatureFlagsToUpdate processFeatureFlagChanges(SplitParser splitParser, List<Split> splits) {
        List<ParsedSplit> toAdd = new ArrayList<>();
        List<String> toRemove = new ArrayList<>();
        Set<String> segments = new HashSet<>();
        for (Split split : splits) {
            if (split.status != Status.ACTIVE) {
                // archive.
                toRemove.add(split.name);
                continue;
            }
            ParsedSplit parsedSplit = splitParser.parse(split);
            if (parsedSplit == null) {
                _log.debug(String.format("We could not parse the feature flag definition for: %s", split.name));
                continue;
            }
            segments.addAll(parsedSplit.getSegmentsNames());
            toAdd.add(parsedSplit);
        }
        return new FeatureFlagsToUpdate(toAdd, toRemove, segments);
    }
}