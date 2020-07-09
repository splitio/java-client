package io.split.engine.segments;

import com.google.common.collect.ImmutableMap;

import java.util.Collections;
import java.util.Map;

/**
 * Provides fetchers of type StaticSegmentFetcher.
 *
 * @author adil
 */
public class StaticSegmentFetcher implements SegmentFetcher {

    private final ImmutableMap<String, StaticSegment> _staticSegmentFetchers;

    public StaticSegmentFetcher(Map<String, StaticSegment> staticSegmentFetchers) {
        _staticSegmentFetchers = ImmutableMap.copyOf(staticSegmentFetchers);
    }


    @Override
    public Segment segment(String segmentName) {
        StaticSegment segmentFetcher = _staticSegmentFetchers.get(segmentName);
        if (segmentFetcher == null) {
            segmentFetcher = new StaticSegment(segmentName, Collections.<String>emptySet());
        }
        return segmentFetcher;
    }

    @Override
    public long getChangeNumber(String segmentName) { return 0; }

    @Override
    public void forceRefresh(String segmentName) { return; }

    @Override
    public void forceRefreshAll() { return; }

    @Override
    public void startPeriodicFetching() { return; }
}
