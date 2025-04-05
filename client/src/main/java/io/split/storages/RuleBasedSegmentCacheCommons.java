package io.split.storages;

import java.util.Set;

public interface RuleBasedSegmentCacheCommons {
    long getChangeNumber();
    Set<String> getSegments();
}
