package io.split.client;

import io.split.client.dtos.SegmentChange;
import io.split.engine.common.FetchOptions;
import io.split.engine.segments.SegmentChangeFetcher;

public class LocalhostSegmentFetcherNoop implements SegmentChangeFetcher {

    @Override
    public SegmentChange fetch(String segmentName, long changesSinceThisChangeNumber, FetchOptions options) {
        return new SegmentChange();
    }
}
