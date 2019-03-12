package io.split.engine.experiments;

import java.util.List;
import java.util.Set;

/**
 * Created by adilaijaz on 5/8/15.
 */
public interface SplitFetcher {
    ParsedSplit fetch(String splitName);

    List<ParsedSplit> fetchAll();

    Set<String> fetchUsedTrafficTypes();

    /**
     * Forces a sync of splits, outside of any scheduled
     * syncs. This method MUST NOT throw any exceptions.
     */
    void forceRefresh();
}
