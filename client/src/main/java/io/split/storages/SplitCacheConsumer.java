package io.split.storages;

import io.split.engine.experiments.ParsedSplit;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface SplitCacheConsumer extends SplitCacheCommons{
    ParsedSplit get(String name);
    Collection<ParsedSplit> getAll();
    Collection<ParsedSplit> fetchMany(List<String> names);
    boolean trafficTypeExists(String trafficTypeName);
}
