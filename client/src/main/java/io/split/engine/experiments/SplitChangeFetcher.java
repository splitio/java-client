package io.split.engine.experiments;

import io.split.client.dtos.SplitChange;
import io.split.client.exceptions.InputStreamProviderException;
import io.split.engine.common.FetchOptions;

/**
 * Created by adilaijaz on 5/11/15.
 */
public interface SplitChangeFetcher {
    /**
     * The returned list should contain AT MOST one split
     * per name. Thus, in the time between requested change number
     * and latest change number, if multiple changes have happened to
     * partitions tied to a name, just return the latest change.
     * <p/>
     * <p/>
     * If no changes have every happened, then return the an empty list of
     * changed partitions, with the latest change number set to a value less than 0.
     * <p/>
     * <p/>
     * If no changes have happened since the change number requested, then return
     * an empty list of changed partitions with the latest change number being the same
     * as the requested change number.
     * <p/>
     * <p/>
     * If the client is asking for split changes for the first time,
     * implementations should only return active partitions. No need to
     * return killed partitions.
     *
     * @param since a value less than zero implies that the client is
     *              requesting information on partitions for the first time.
     * @return SegmentChange
     * @throws java.lang.RuntimeException if there was a problem computing split changes
     */
    SplitChange fetch(long since, FetchOptions options) throws InputStreamProviderException;
}
