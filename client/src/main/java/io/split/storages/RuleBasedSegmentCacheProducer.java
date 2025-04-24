package io.split.storages;

import io.split.engine.experiments.ParsedRuleBasedSegment;

import java.util.List;

public interface RuleBasedSegmentCacheProducer extends  RuleBasedSegmentCacheCommons{
    boolean remove(String name);
    void setChangeNumber(long changeNumber);
    void update(List<ParsedRuleBasedSegment> toAdd, List<String> toRemove, long changeNumber);
    void clear();
}
