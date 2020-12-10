package io.split.engine.cache;

import io.split.engine.experiments.ParsedSplit;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface SplitCache {
    void put(ParsedSplit split);
    boolean remove(String name);
    ParsedSplit get(String name);
    Collection<ParsedSplit> getAll();
    Collection<ParsedSplit> getMany(List<String> names);
    long getChangeNumber();
    void setChangeNumber(long changeNumber);
    boolean trafficTypeExists(String trafficTypeName);
    void clear();
}
