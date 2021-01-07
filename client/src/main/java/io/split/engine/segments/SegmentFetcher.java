package io.split.engine.segments;

/**
 * Created by adilaijaz on 5/7/15.
 */
public interface SegmentFetcher {
    /**
     * fetch
     */
    void fetch();

    /**
     * forceRefresh
     */
    void forceRefresh();
}
