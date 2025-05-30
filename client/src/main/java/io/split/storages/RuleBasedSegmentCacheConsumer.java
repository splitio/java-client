package io.split.storages;

import io.split.engine.experiments.ParsedRuleBasedSegment;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface RuleBasedSegmentCacheConsumer {
    ParsedRuleBasedSegment get(String name);
    Collection<ParsedRuleBasedSegment> getAll();
    List<String> ruleBasedSegmentNames();
    boolean contains(Set<String> ruleBasedSegmentNames);
    long getChangeNumber();
    Set<String> getSegments();
}