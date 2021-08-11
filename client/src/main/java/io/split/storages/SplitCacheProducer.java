package io.split.storages;

import io.split.engine.experiments.ParsedSplit;

import java.util.List;

public interface SplitCacheProducer extends  SplitCacheCommons{
    boolean remove(String name);
    void setChangeNumber(long changeNumber);
    void kill(String splitName, String defaultTreatment, long changeNumber);
    void clear();
    void putMany(List<ParsedSplit> splits, long changeNumber);
    void increaseTrafficType(String trafficType);
    void decreaseTrafficType(String trafficType);
}
