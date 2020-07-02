package io.split.engine.experiments;

import java.util.List;
import java.util.Set;

/**
 * Created by adilaijaz on 5/8/15.
 */
public interface SplitFetcher {
    ParsedSplit fetch(String splitName);

    List<ParsedSplit> fetchAll();

    /**
     * Fetches all the traffic types that are being used by the splits that are currently stored.
     *
     * For example, if the fetcher currently contains three splits, one of traffic type "account"
     * and two of traffic type "user", this method will return ["account", "user"]
     *
     * @return a set of all the traffic types used by the parsed splits
     */
    Set<String> fetchKnownTrafficTypes();

    /**
     * Forces a sync of splits, outside of any scheduled
     * syncs. This method MUST NOT throw any exceptions.
     */
    void forceRefresh();

    long changeNumber();

    void killSplit(String splitName, String defaultTreatment, long changeNumber);
}
