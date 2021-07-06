package io.split.storages;

import io.split.engine.experiments.ParsedSplit;

import java.util.Collection;
import java.util.List;

public interface SplitCacheConsumer {
    ParsedSplit get(String name);
    Collection<ParsedSplit> getAll();
    Collection<ParsedSplit> getMany(List<String> names);
    long getChangeNumber();
    boolean trafficTypeExists(String trafficTypeName);
}
