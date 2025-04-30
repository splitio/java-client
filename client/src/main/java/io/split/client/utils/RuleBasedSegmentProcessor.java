package io.split.client.utils;

import io.split.client.dtos.Excluded;
import io.split.client.dtos.RuleBasedSegment;
import io.split.client.dtos.Status;
import io.split.engine.experiments.ParsedRuleBasedSegment;
import io.split.engine.experiments.RuleBasedSegmentParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RuleBasedSegmentProcessor {
    private static final Logger _log = LoggerFactory.getLogger(RuleBasedSegmentProcessor.class);

    public static RuleBasedSegmentsToUpdate processRuleBasedSegmentChanges(RuleBasedSegmentParser ruleBasedSegmentParser,
                                                                           List<RuleBasedSegment> ruleBasedSegments) {
        List<ParsedRuleBasedSegment> toAdd = new ArrayList<>();
        List<String> toRemove = new ArrayList<>();
        Set<String> segments = new HashSet<>();
        for (RuleBasedSegment ruleBasedSegment : ruleBasedSegments) {
            ruleBasedSegment.excluded = checkExcluded(ruleBasedSegment.excluded);
            if (ruleBasedSegment.status != Status.ACTIVE) {
                // archive.
                toRemove.add(ruleBasedSegment.name);
                continue;
            }
            ParsedRuleBasedSegment parsedRuleBasedSegment = ruleBasedSegmentParser.parse(ruleBasedSegment);
            if (parsedRuleBasedSegment == null) {
                _log.debug(String.format("We could not parse the rule based segment definition for: %s", ruleBasedSegment.name));
                continue;
            }
            segments.addAll(parsedRuleBasedSegment.getSegmentsNames());
            toAdd.add(parsedRuleBasedSegment);
        }
        return new RuleBasedSegmentsToUpdate(toAdd, toRemove, segments);
    }

    private static Excluded createEmptyExcluded() {
        Excluded excluded = new Excluded();
        excluded.segments = new ArrayList<>();
        excluded.keys = new ArrayList<>();
        return excluded;
    }

    private static Excluded checkExcluded(Excluded excluded) {
        if (excluded == null) {
            excluded = createEmptyExcluded();
        }
        if (excluded.segments == null) {
            excluded.segments = new ArrayList<>();
        }
        if (excluded.keys == null) {
            excluded.keys = new ArrayList<>();
        }
        return excluded;
    }
}