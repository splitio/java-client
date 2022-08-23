package io.split.engine.segments;

import io.split.engine.common.FetchOptions;

/**
 * Created by adilaijaz on 5/7/15.
 */
public interface SegmentFetcher {
    /**
     * fetch
     */
    void fetch(FetchOptions opts);

    boolean runWhitCacheHeader();
}
