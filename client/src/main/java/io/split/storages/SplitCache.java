package io.split.storages;

import io.split.engine.experiments.ParsedSplit;

import java.util.Collection;
import java.util.List;

public interface SplitCache {
    void put(ParsedSplit split);
    boolean remove(String name);
    ParsedSplit get(String name);
    Collection<ParsedSplit> getAll();
    Collection<ParsedSplit> getMany(List<String> names);
    long getChangeNumber();
    void setChangeNumber(long changeNumber);
    boolean trafficTypeExists(String trafficTypeName);
    void kill(String splitName, String defaultTreatment, long changeNumber);
    void clear();
}
