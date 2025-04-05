package io.split.storages;

import io.split.engine.experiments.ParsedRuleBasedSegment;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface RuleBasedSegmentCacheConsumer extends RuleBasedSegmentCacheCommons {
    ParsedRuleBasedSegment get(String name);
    Collection<ParsedRuleBasedSegment> getAll();
    List<String> ruleBasedSegmentNames();
}