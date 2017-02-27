package io.split.engine.experiments;

import java.util.List;

/**
 * Created by adilaijaz on 5/8/15.
 */
public interface SplitFetcher {
    ParsedSplit fetch(String splitName);

    List<ParsedSplit> fetchAll();

    /**
     * Forces a sync of splits, outside of any scheduled
     * syncs. This method MUST NOT throw any exceptions.
     */
    void forceRefresh();
}
