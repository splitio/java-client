package io.split.engine.segments;

import io.split.client.dtos.SegmentChange;
import io.split.engine.common.FetchOptions;

/**
 * Fetches changes in the segment since a reference point.
 *
 * @author adil
 */
public interface SegmentChangeFetcher {
    /**
     * <p/>
     * <p/>
     * If the segment does not exist, then return the an empty segment change
     * with the latest change number set to a value less than 0.
     * <p/>
     * <p/>
     * If no changes have happened since the change number requested, then return
     * an empty segment change with the latest change number equal to the requested
     * change number.
     *
     * @param segmentName                  the name of the segment to fetch.
     * @param changesSinceThisChangeNumber a value less than zero implies that the client is
     *                                     requesting information on this segment for the first time.
     * @param options
     * @return SegmentChange
     * @throws java.lang.RuntimeException if there was a problem fetching segment changes
     */
    SegmentChange fetch(String segmentName, long changesSinceThisChangeNumber, FetchOptions options);
}
