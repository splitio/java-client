package io.split.engine.cache;

import io.split.engine.experiments.ParsedSplit;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface SplitCache {
    void put(ParsedSplit split);
    void putAll(Map<String, ParsedSplit> splits);
    boolean remove(String name);
    ParsedSplit get(String name);
    Collection<ParsedSplit> getAll();
    Collection<ParsedSplit> getMany(List<String> names);
    long getChangeNumber();
    void setChangeNumber(long changeNumber);
    boolean trafficTypeExists(String trafficType);
    void clear();
}
