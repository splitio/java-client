package io.split.storages;

import io.split.engine.experiments.ParsedSplit;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface SplitCacheConsumer extends SplitCacheCommons{
    ParsedSplit get(String name);
    Collection<ParsedSplit> getAll();
    Map<String, ParsedSplit> fetchMany(List<String> names);
    boolean trafficTypeExists(String trafficTypeName);
    List<String> splitNames();
}
