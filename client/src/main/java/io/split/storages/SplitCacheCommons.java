package io.split.storages;

import java.util.Set;

public interface SplitCacheCommons {
    long getChangeNumber();
    Set<String> getSegments();
}
