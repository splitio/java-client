package io.split.storages;

import io.split.engine.experiments.ParsedSplit;

public interface SplitCacheProducer {
    void put(ParsedSplit split);
    boolean remove(String name);
    void setChangeNumber(long changeNumber);
    void kill(String splitName, String defaultTreatment, long changeNumber);
    void clear();
}
